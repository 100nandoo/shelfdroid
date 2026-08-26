package dev.halim.shelfdroid.core.data.screen.libraryadministration

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateLibraryRequest
import dev.halim.core.network.request.ReorderLibraryRequest
import dev.halim.core.network.request.ValidateCronRequest
import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryDataSyncResult
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryContract
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import dev.halim.shelfdroid.core.database.LibraryEntity
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import retrofit2.HttpException

class LibraryAdministrationRepository
@Inject
constructor(
  private val api: ApiService,
  private val libraryRepository: LibraryRepository,
  private val libraryItemRepository: LibraryItemRepository,
  private val libraryDataRepository: LibraryDataRepository,
  private val mutationCoordinator: LibraryMutationCoordinator,
  private val serverTaskRepository: ServerTaskRepositoryContract,
  private val libraryEventRepository: LibraryAdministrationEventRepository,
) : LibraryAdministrationContract, LibraryAdministrationCreateContract {

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

  override suspend fun loadLibraries(): Result<List<LibraryAdministrationLibrary>> {
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
    libraries: List<LibraryAdministrationLibrary>
  ): Result<LibraryAdministrationMutationResult<List<LibraryAdministrationLibrary>>> =
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
          LibraryAdministrationMutationResult.AcceptedButNotSynchronized(
            value = acceptedOrder,
            error = persistenceFailure,
          )
        )
      }

      val synchronization = libraryDataRepository.synchronize()
      if (synchronization.isSuccess) {
        Result.success(LibraryAdministrationMutationResult.Accepted(acceptedOrder))
      } else {
        Result.success(
          LibraryAdministrationMutationResult.AcceptedButNotSynchronized(
            value = acceptedOrder,
            error = synchronization.error
              ?: IllegalStateException("Library data synchronization failed"),
          )
        )
      }
    }

  override suspend fun deleteLibrary(libraryId: String): Result<Unit> =
    mutationCoordinator.withMutation {
      api.deleteLibrary(libraryId).map {
        libraryEventRepository.registerLocalMutation(
          LibraryAdministrationLibraryEventType.REMOVED,
          it,
        )
        // The server owns rich Library configuration and media files. ShelfDroid only removes
        // the catalog projection, preserving buffered playback and downloaded media.
        libraryItemRepository.removeLibraryFromCatalog(libraryId)
        libraryRepository.removeFromCatalog(libraryId)
      }
    }

  override suspend fun loadLibraryProviders(
    mediaType: LibraryAdministrationMediaType
  ): Result<List<LibraryAdministrationProvider>> {
    return api.searchProviders().map { response ->
      val providers =
        when (mediaType) {
          LibraryAdministrationMediaType.BOOK -> response.providers.books
          LibraryAdministrationMediaType.PODCAST -> response.providers.podcasts
          LibraryAdministrationMediaType.UNKNOWN -> emptyList()
        }
      providers
        .asSequence()
        .filter { it.value.isNotBlank() }
        .map { LibraryAdministrationProvider(id = it.value, name = it.text.ifBlank { it.value }) }
        .toList()
    }
  }

  override suspend fun browseLibraryFilesystem(path: String?): Result<LibraryAdministrationFilesystem> {
    return api.filesystem(path = path).map { response ->
      LibraryAdministrationFilesystem(
        isPosix = response.posix,
        directories =
          response.directories.map {
            LibraryAdministrationDirectory(
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
            LibraryAdministrationScheduleValidationException.Invalid(error.message())
          } else {
            LibraryAdministrationScheduleValidationException.Unavailable(error.message)
          }
        )
      },
    )
  }

  override suspend fun createLibrary(
    draft: LibraryAdministrationDraft
  ): Result<LibraryAdministrationCreateResult> {
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
        LibraryAdministrationLibraryEventType.ADDED,
        serverLibrary,
      )
      val administrationLibrary = serverLibrary.toAdministrationLibrary()
      libraryRepository.refreshLibraries().fold(
        onSuccess = {
          Result.success(LibraryAdministrationCreateResult.Created(administrationLibrary))
        },
        onFailure = { error ->
          Result.success(
            LibraryAdministrationCreateResult.CreatedButNotSynchronized(
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

private fun LibraryDataSyncResult.toResult(): Result<Unit> {
  if (isSuccess) return Result.success(Unit)
  return Result.failure(error ?: IllegalStateException("Library data synchronization failed"))
}

private fun LibraryEntity.toAdministrationLibrary(): LibraryAdministrationLibrary =
  LibraryAdministrationLibrary(
    id = id,
    name = name,
    mediaType =
      if (isBookLibrary == 1L) {
        LibraryAdministrationMediaType.BOOK
      } else {
        LibraryAdministrationMediaType.PODCAST
      },
    displayOrder = displayOrder.toInt(),
  )

private fun Library.toAdministrationLibrary(): LibraryAdministrationLibrary =
  LibraryAdministrationLibrary(
    id = id,
    name = name,
    mediaType =
      when (mediaType) {
        MediaType.BOOK -> LibraryAdministrationMediaType.BOOK
        MediaType.PODCAST -> LibraryAdministrationMediaType.PODCAST
        MediaType.UNKNOWN -> LibraryAdministrationMediaType.UNKNOWN
      },
    displayOrder = displayOrder,
  )

private fun LibraryAdministrationMediaType.toApiValue(): String =
  when (this) {
    LibraryAdministrationMediaType.BOOK -> "book"
    LibraryAdministrationMediaType.PODCAST -> "podcast"
    LibraryAdministrationMediaType.UNKNOWN -> "book"
  }

private fun LibraryAdministrationDraft.toCreateSettings(): CreateLibraryRequest.Settings =
  when (mediaType) {
    LibraryAdministrationMediaType.BOOK ->
      CreateLibraryRequest.Settings(
        coverAspectRatio = bookSettings.coverAspectRatio,
        disableWatcher = bookSettings.disableWatcher,
        audiobooksOnly = bookSettings.audiobooksOnly,
        skipMatchingMediaWithAsin = bookSettings.skipMatchingMediaWithAsin,
        skipMatchingMediaWithIsbn = bookSettings.skipMatchingMediaWithIsbn,
        epubsAllowScriptedContent = bookSettings.epubsAllowScriptedContent,
        hideSingleBookSeries = bookSettings.hideSingleBookSeries,
        onlyShowLaterBooksInContinueSeries = bookSettings.onlyShowLaterBooksInContinueSeries,
        markAsFinishedPercentComplete = bookSettings.markAsFinishedPercentComplete,
        markAsFinishedTimeRemaining = bookSettings.markAsFinishedTimeRemaining,
        metadataPrecedence = metadataPrecedence,
        autoScanCronExpression = scheduleExpressionOrNull(),
      )
    LibraryAdministrationMediaType.PODCAST ->
      CreateLibraryRequest.Settings(
        coverAspectRatio = podcastSettings.coverAspectRatio,
        disableWatcher = podcastSettings.disableWatcher,
        podcastSearchRegion = podcastSettings.podcastSearchRegion,
        markAsFinishedPercentComplete = podcastSettings.markAsFinishedPercentComplete,
        markAsFinishedTimeRemaining = podcastSettings.markAsFinishedTimeRemaining,
        autoScanCronExpression = scheduleExpressionOrNull(),
      )
    LibraryAdministrationMediaType.UNKNOWN -> CreateLibraryRequest.Settings()
  }
