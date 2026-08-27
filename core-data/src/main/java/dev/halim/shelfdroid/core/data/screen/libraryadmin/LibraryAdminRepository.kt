package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateLibraryRequest
import dev.halim.core.network.request.ReorderLibraryRequest
import dev.halim.core.network.request.ValidateCronRequest
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryDataSyncResult
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryContract
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import dev.halim.shelfdroid.core.database.LibraryEntity
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class LibraryAdminRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryRepository: LibraryRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val libraryDataRepository: LibraryDataRepository,
  private val mutationCoordinator: LibraryMutationCoordinator,
  private val serverTaskRepository: ServerTaskRepositoryContract,
  private val libraryEventRepository: LibraryAdminEventRepository,
) : LibraryAdminContract, LibraryAdminCreateContract {

  override val libraryEvents = libraryEventRepository.events

  override val taskState: StateFlow<ServerTaskRepositoryState>
    get() = serverTaskRepository.state

  override val taskNotifications: StateFlow<ServerTaskNotification?>
    get() = serverTaskRepository.notifications

  override fun acknowledgeTaskNotification(taskId: String) {
    serverTaskRepository.acknowledgeNotification(taskId)
  }

  override suspend fun refreshTasks(): Result<Unit> = serverTaskRepository.refresh()

  override suspend fun startScan(libraryId: String): Result<Unit> =
    serverTaskRepository.startLibraryScan(libraryId)

  override suspend fun startMatch(libraryId: String): Result<Unit> =
    serverTaskRepository.startLibraryMatch(libraryId)

  override suspend fun retryTaskSynchronization(taskId: String): Result<Unit> =
    serverTaskRepository.retrySynchronization(taskId)

  override suspend fun loadLibraries(): Result<List<LibraryAdminLibrary>> {
    // Explicit refresh is the same authoritative Library data boundary used by socket events. It
    // refreshes Libraries before their Library items and only exposes the local projection after
    // the complete synchronization succeeds.
    return mutationCoordinator.withMutation {
      libraryDataRepository.synchronize().toResult().map {
        libraryRepository.listLibraries().map { library ->
          library.toAdministrationLibrary()
        }
      }
    }
  }

  override suspend fun reorderLibraries(
    libraries: List<LibraryAdminLibrary>
  ): Result<LibraryAdminMutationResult<List<LibraryAdminLibrary>>> =
    mutationCoordinator.withMutation {
      val response =
        api
          .reorderLibraries(
            libraries.mapIndexed { index, library ->
              ReorderLibraryRequest(id = library.id, newOrder = index + 1)
            }
          )
          .getOrElse { return@withMutation Result.failure(it) }
      val acceptedOrder = response.libraries.map { it.toAdministrationLibrary() }

      // Persist the accepted order before synchronizing items. If the follow-up synchronization
      // cannot reach the server, the accepted server order is still the local administration
      // projection and can be retried without repeating the reorder request.
      val persistenceFailure =
        try {
          // Rich administration settings remain server-backed and are deliberately not copied
          // into local storage.
          libraryRepository.persistLibraries(response.libraries)
          null
        } catch (error: Throwable) {
          if (error is kotlinx.coroutines.CancellationException) throw error
          error
        }
      if (persistenceFailure != null) {
        return@withMutation Result.success(
          LibraryAdminMutationResult.AcceptedButNotSynchronized(
            value = acceptedOrder,
            error = persistenceFailure,
          )
        )
      }

      val synchronization = libraryDataRepository.synchronize()
      if (synchronization.isSuccess) {
        Result.success(LibraryAdminMutationResult.Accepted(acceptedOrder))
      } else {
        Result.success(
          LibraryAdminMutationResult.AcceptedButNotSynchronized(
            value = acceptedOrder,
            error = synchronization.error
              ?: IllegalStateException("Library data synchronization failed"),
          )
        )
      }
    }

  override suspend fun deleteLibrary(
    libraryId: String
  ): Result<LibraryAdminMutationResult<Unit>> =
    mutationCoordinator.withMutation {
      val deletedLibrary =
        api.deleteLibrary(libraryId).getOrElse { return@withMutation Result.failure(it) }
      Result.success(
        runAcceptedLibraryDeleteMutation {
          libraryEventRepository.registerLocalMutation(
            LibraryAdminLibraryEventType.REMOVED,
            deletedLibrary,
          )
          // The server owns rich Library configuration and media files. ShelfDroid only removes the
          // catalog projection, preserving buffered playback and downloaded media.
          libraryItemRepository.removeLibraryFromCatalog(libraryId)
          libraryRepository.removeFromCatalog(libraryId)

          val synchronization = libraryDataRepository.synchronize()
          if (synchronization.isSuccess) {
            LibraryAdminMutationResult.Accepted(Unit)
          } else {
            LibraryAdminMutationResult.AcceptedButNotSynchronized(
              value = Unit,
              error = synchronization.error
                ?: IllegalStateException("Library data synchronization failed"),
            )
          }
        }
      )
    }

  override suspend fun loadLibraryProviders(
    mediaType: LibraryAdminMediaType
  ): Result<List<LibraryAdminProvider>> {
    return api.searchProviders().map { response ->
      val providers =
        when (mediaType) {
          LibraryAdminMediaType.BOOK -> response.providers.books
          LibraryAdminMediaType.PODCAST -> response.providers.podcasts
          LibraryAdminMediaType.UNKNOWN -> emptyList()
        }
      providers
        .asSequence()
        .filter { it.value.isNotBlank() }
        .map { LibraryAdminProvider(id = it.value, name = it.text.ifBlank { it.value }) }
        .toList()
    }
  }

  override suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdminFilesystem> {
    return api.filesystem(path = path).map { response ->
      LibraryAdminFilesystem(
        isPosix = response.posix,
        directories =
          response.directories.map {
            LibraryAdminDirectory(
              path = it.path,
              name = it.dirname.ifBlank { it.path.substringAfterLast('/') },
              level = it.level,
            )
          },
      )
    }
  }

  override suspend fun validateLibrarySchedule(expression: String): Result<Unit> {
    return api.validateCron(ValidateCronRequest(expression)).fold(
      onSuccess = { Result.success(Unit) },
      onFailure = { error ->
        Result.failure(
          if (error is HttpException && error.code() == 400) {
            LibraryAdminScheduleValidationException.Invalid(error.message())
          } else {
            LibraryAdminScheduleValidationException.Unavailable(error.message)
          }
        )
      },
    )
  }

  override suspend fun createLibrary(
    draft: LibraryAdminDraft
  ): Result<LibraryAdminCreateResult> {
    return mutationCoordinator.withMutation {
      val serverLibrary =
        api
          .createLibrary(
            CreateLibraryRequest(
              name = draft.name.trim(),
              folders =
                draft.folders.map { path ->
                  CreateLibraryRequest.Folder(normalizeLibraryFolderPath(path))
                },
              mediaType = draft.mediaType.toApiValue(),
              icon = draft.icon,
              provider = draft.provider.orEmpty(),
              settings = draft.toCreateSettings(),
            )
          )
          .getOrElse { return@withMutation Result.failure(it) }

      libraryEventRepository.registerLocalMutation(
        LibraryAdminLibraryEventType.ADDED,
        serverLibrary,
      )
      val administrationLibrary = serverLibrary.toAdministrationLibrary()
      libraryRepository.refreshLibraries().fold(
        onSuccess = {
          Result.success(LibraryAdminCreateResult.Created(administrationLibrary))
        },
        onFailure = { error ->
          Result.success(
            LibraryAdminCreateResult.CreatedButNotSynchronized(
              library = administrationLibrary,
              error = error,
            )
          )
        },
      )
    }
  }

  override suspend fun synchronizeLibraries(): Result<Unit> =
    mutationCoordinator.withMutation { libraryDataRepository.synchronize().toResult() }
}

/** Keeps an accepted delete successful when any local follow-up step needs synchronization retry. */
internal suspend fun runAcceptedLibraryDeleteMutation(
  operation: suspend () -> LibraryAdminMutationResult<Unit>,
): LibraryAdminMutationResult<Unit> =
  try {
    operation()
  } catch (error: Throwable) {
    if (error is CancellationException) throw error
    LibraryAdminMutationResult.AcceptedButNotSynchronized(
      value = Unit,
      error = error,
    )
  }

private fun LibraryDataSyncResult.toResult(): Result<Unit> {
  if (isSuccess) return Result.success(Unit)
  return Result.failure(error ?: IllegalStateException("Library data synchronization failed"))
}

private fun LibraryEntity.toAdministrationLibrary(): LibraryAdminLibrary =
  LibraryAdminLibrary(
    id = id,
    name = name,
    mediaType =
      if (isBookLibrary == 1L) {
        LibraryAdminMediaType.BOOK
      } else {
        LibraryAdminMediaType.PODCAST
      },
    displayOrder = displayOrder.toInt(),
  )

private fun LibraryAdminMediaType.toApiValue(): String =
  when (this) {
    LibraryAdminMediaType.BOOK -> "book"
    LibraryAdminMediaType.PODCAST -> "podcast"
    LibraryAdminMediaType.UNKNOWN -> "book"
  }

internal fun LibraryAdminDraft.toCreateSettings(): CreateLibraryRequest.Settings =
  when (mediaType) {
    LibraryAdminMediaType.BOOK ->
      CreateLibraryRequest.Settings(
        coverAspectRatio = bookSettings.coverAspectRatio,
        disableWatcher = bookSettings.disableWatcher,
        audiobooksOnly = bookSettings.audiobooksOnly,
        skipMatchingMediaWithAsin = bookSettings.skipMatchingMediaWithAsin,
        skipMatchingMediaWithIsbn = bookSettings.skipMatchingMediaWithIsbn,
        epubsAllowScriptedContent = bookSettings.epubsAllowScriptedContent,
        hideSingleBookSeries = bookSettings.hideSingleBookSeries,
        onlyShowLaterBooksInContinueSeries = bookSettings.onlyShowLaterBooksInContinueSeries,
        markAsFinishedPercentComplete = bookSettings.finishThreshold.serializedPercentComplete,
        markAsFinishedTimeRemaining = bookSettings.finishThreshold.serializedTimeRemaining,
        metadataPrecedence = metadataPrecedence,
        autoScanCronExpression = scheduleExpressionOrNull(),
      )
    LibraryAdminMediaType.PODCAST ->
      CreateLibraryRequest.Settings(
        coverAspectRatio = podcastSettings.coverAspectRatio,
        disableWatcher = podcastSettings.disableWatcher,
        podcastSearchRegion = podcastSettings.podcastSearchRegion,
        markAsFinishedPercentComplete = podcastSettings.finishThreshold.serializedPercentComplete,
        markAsFinishedTimeRemaining = podcastSettings.finishThreshold.serializedTimeRemaining,
        autoScanCronExpression = scheduleExpressionOrNull(),
      )
    LibraryAdminMediaType.UNKNOWN -> CreateLibraryRequest.Settings()
  }
