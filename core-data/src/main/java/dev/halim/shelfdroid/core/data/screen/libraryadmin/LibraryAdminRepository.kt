package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreateLibraryRequest
import dev.halim.core.network.request.ReorderLibraryRequest
import dev.halim.core.network.request.UpdateLibraryRequest
import dev.halim.core.network.request.ValidateCronRequest
import dev.halim.core.network.response.Library
import dev.halim.core.network.response.toDomain
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryDataSyncResult
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.*
import dev.halim.shelfdroid.core.data.task.ServerTaskNotification
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryContract
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryState
import dev.halim.shelfdroid.core.database.LibraryEntity
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
          .getOrElse {
            return@withMutation Result.failure(it)
          }
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
            error =
              synchronization.error ?: IllegalStateException("Library data synchronization failed"),
          )
        )
      }
    }

  override suspend fun deleteLibrary(libraryId: String): Result<LibraryAdminMutationResult<Unit>> =
    mutationCoordinator.withMutation {
      val deletedLibrary =
        api.deleteLibrary(libraryId).getOrElse {
          return@withMutation Result.failure(it)
        }
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
              error =
                synchronization.error
                  ?: IllegalStateException("Library data synchronization failed"),
            )
          }
        }
      )
    }

  override suspend fun loadLibraryProviders(
    mediaType: MediaType
  ): Result<List<LibraryAdminProvider>> {
    return api.searchProviders().map { response ->
      val providers =
        when (mediaType) {
          MediaType.BOOK -> response.providers.books
          MediaType.PODCAST -> response.providers.podcasts
          MediaType.UNKNOWN -> emptyList()
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
    return api
      .validateCron(ValidateCronRequest(expression))
      .fold(
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

  override suspend fun createLibrary(draft: LibraryAdminDraft): Result<LibraryAdminCreateResult> {
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
          .getOrElse {
            return@withMutation Result.failure(it)
          }

      finalizeLibraryMutation(
        eventType = LibraryAdminLibraryEventType.ADDED,
        serverLibrary = serverLibrary,
        synchronized = LibraryAdminCreateResult::Created,
        notSynchronized = LibraryAdminCreateResult::CreatedButNotSynchronized,
      )
    }
  }

  override suspend fun loadLibrary(libraryId: String): Result<LibraryAdminEditSnapshot> =
    api.libraries().mapCatching { response ->
      response.libraries.firstOrNull { it.id == libraryId }?.toEditSnapshot()
        ?: error("Library not found")
    }

  override suspend fun updateLibrary(
    libraryId: String,
    original: LibraryAdminEditSnapshot,
    draft: LibraryAdminDraft,
  ): Result<LibraryAdminUpdateResult> = mutationCoordinator.withMutation {
    val serverLibrary =
      api.updateLibrary(libraryId, buildUpdateLibraryRequest(original, draft)).getOrElse {
        return@withMutation Result.failure(it)
      }

    finalizeLibraryMutation(
      eventType = LibraryAdminLibraryEventType.UPDATED,
      serverLibrary = serverLibrary,
      synchronized = LibraryAdminUpdateResult::Updated,
      notSynchronized = LibraryAdminUpdateResult::UpdatedButNotSynchronized,
    )
  }

  private suspend fun <T> finalizeLibraryMutation(
    eventType: LibraryAdminLibraryEventType,
    serverLibrary: Library,
    synchronized: (LibraryAdminLibrary) -> T,
    notSynchronized: (LibraryAdminLibrary, Throwable) -> T,
  ): Result<T> {
    libraryEventRepository.registerLocalMutation(eventType, serverLibrary)
    val administrationLibrary = serverLibrary.toAdministrationLibrary()
    return libraryDataRepository
      .synchronize()
      .toResult()
      .fold(
        onSuccess = { Result.success(synchronized(administrationLibrary)) },
        onFailure = { error ->
          Result.success(notSynchronized(administrationLibrary, error))
        },
      )
  }

  override suspend fun synchronizeLibraries(): Result<Unit> = mutationCoordinator.withMutation {
    libraryDataRepository.synchronize().toResult()
  }
}

/**
 * Keeps an accepted delete successful when any local follow-up step needs synchronization retry.
 */
internal suspend fun runAcceptedLibraryDeleteMutation(
  operation: suspend () -> LibraryAdminMutationResult<Unit>
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
        MediaType.BOOK
      } else {
        MediaType.PODCAST
      },
    displayOrder = displayOrder.toInt(),
    icon = icon,
  )

private fun MediaType.toApiValue(): String = apiValue ?: "book"

internal fun LibraryAdminDraft.toCreateSettings(): CreateLibraryRequest.Settings =
  when (mediaType) {
    MediaType.BOOK ->
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
    MediaType.PODCAST ->
      CreateLibraryRequest.Settings(
        coverAspectRatio = podcastSettings.coverAspectRatio,
        disableWatcher = podcastSettings.disableWatcher,
        podcastSearchRegion = podcastSettings.podcastSearchRegion,
        markAsFinishedPercentComplete = podcastSettings.finishThreshold.serializedPercentComplete,
        markAsFinishedTimeRemaining = podcastSettings.finishThreshold.serializedTimeRemaining,
        autoScanCronExpression = scheduleExpressionOrNull(),
      )
    MediaType.UNKNOWN -> CreateLibraryRequest.Settings()
  }

internal fun Library.toEditSnapshot(): LibraryAdminEditSnapshot {
  val type = mediaType.toDomain()
  val sourcesById = DEFAULT_LIBRARY_METADATA_SOURCES.associateBy { it.id }
  val precedence =
    settings.metadataPrecedence.ifEmpty {
      DEFAULT_LIBRARY_METADATA_SOURCES.asReversed().map { it.id }
    }
  val enabledSources =
    precedence.asReversed().mapNotNull { id -> sourcesById[id]?.copy(enabled = true) }
  val disabledSources =
    DEFAULT_LIBRARY_METADATA_SOURCES.filterNot { source -> precedence.contains(source.id) }
      .map { it.copy(enabled = false) }
  val folderPaths = folders.map { normalizeLibraryFolderPath(it.fullPath) }
  val draft =
    LibraryAdminDraft(
      mediaType = type,
      name = name,
      icon = icon.ifBlank { DEFAULT_LIBRARY_ICON },
      folders = folderPaths,
      bookProvider = provider.takeIf { type == MediaType.BOOK },
      podcastProvider = provider.takeIf { type == MediaType.PODCAST },
      bookSettings =
        LibraryAdminBookSettings(
          coverAspectRatio = settings.coverAspectRatio,
          disableWatcher = settings.disableWatcher,
          audiobooksOnly = settings.audiobooksOnly,
          skipMatchingMediaWithAsin = settings.skipMatchingMediaWithAsin,
          skipMatchingMediaWithIsbn = settings.skipMatchingMediaWithIsbn,
          epubsAllowScriptedContent = settings.epubsAllowScriptedContent,
          hideSingleBookSeries = settings.hideSingleBookSeries,
          onlyShowLaterBooksInContinueSeries = settings.onlyShowLaterBooksInContinueSeries,
          markAsFinishedPercentComplete = settings.markAsFinishedPercentComplete,
          markAsFinishedTimeRemaining = settings.markAsFinishedTimeRemaining,
        ),
      podcastSettings =
        LibraryAdminPodcastSettings(
          coverAspectRatio = settings.coverAspectRatio,
          disableWatcher = settings.disableWatcher,
          podcastSearchRegion = settings.podcastSearchRegion,
          markAsFinishedPercentComplete = settings.markAsFinishedPercentComplete,
          markAsFinishedTimeRemaining = settings.markAsFinishedTimeRemaining,
        ),
      schedule = libraryAdminScheduleDraft(settings.autoScanCronExpression),
      metadataSources = enabledSources + disabledSources,
    )
  return LibraryAdminEditSnapshot(
    draft = draft,
    folderIdsByPath = folders.associate { normalizeLibraryFolderPath(it.fullPath) to it.id },
  )
}

internal fun buildUpdateLibraryRequest(
  original: LibraryAdminEditSnapshot,
  draft: LibraryAdminDraft,
): UpdateLibraryRequest {
  val before = original.draft
  val normalizedFolders = draft.folders.map(::normalizeLibraryFolderPath)
  val foldersChanged = normalizedFolders != before.folders.map(::normalizeLibraryFolderPath)
  return UpdateLibraryRequest(
    name = draft.name.trim().takeIf { it != before.name },
    folders =
      normalizedFolders
        .takeIf { foldersChanged }
        ?.map { path ->
          UpdateLibraryRequest.Folder(id = original.folderIdsByPath[path], path = path)
        },
    icon = draft.icon.takeIf { it != before.icon },
    provider = draft.provider?.takeIf { it != before.provider },
    settings = buildChangedSettings(before, draft).takeIf { it.isNotEmpty() },
  )
}

private fun buildChangedSettings(before: LibraryAdminDraft, after: LibraryAdminDraft): JsonObject =
  buildJsonObject {
    if (after.mediaType == MediaType.BOOK) {
      val old = before.bookSettings
      val new = after.bookSettings
      putChanged("coverAspectRatio", old.coverAspectRatio, new.coverAspectRatio)
      putChanged("disableWatcher", old.disableWatcher, new.disableWatcher)
      putChanged("audiobooksOnly", old.audiobooksOnly, new.audiobooksOnly)
      putChanged(
        "skipMatchingMediaWithAsin",
        old.skipMatchingMediaWithAsin,
        new.skipMatchingMediaWithAsin,
      )
      putChanged(
        "skipMatchingMediaWithIsbn",
        old.skipMatchingMediaWithIsbn,
        new.skipMatchingMediaWithIsbn,
      )
      putChanged(
        "epubsAllowScriptedContent",
        old.epubsAllowScriptedContent,
        new.epubsAllowScriptedContent,
      )
      putChanged("hideSingleBookSeries", old.hideSingleBookSeries, new.hideSingleBookSeries)
      putChanged(
        "onlyShowLaterBooksInContinueSeries",
        old.onlyShowLaterBooksInContinueSeries,
        new.onlyShowLaterBooksInContinueSeries,
      )
      putChangedNullable(
        "markAsFinishedPercentComplete",
        old.finishThreshold.serializedPercentComplete,
        new.finishThreshold.serializedPercentComplete,
      )
      putChangedNullable(
        "markAsFinishedTimeRemaining",
        old.finishThreshold.serializedTimeRemaining,
        new.finishThreshold.serializedTimeRemaining,
      )
      if (before.metadataPrecedence != after.metadataPrecedence) {
        put(
          "metadataPrecedence",
          kotlinx.serialization.json.JsonArray(after.metadataPrecedence.map(::JsonPrimitive)),
        )
      }
    } else {
      val old = before.podcastSettings
      val new = after.podcastSettings
      putChanged("coverAspectRatio", old.coverAspectRatio, new.coverAspectRatio)
      putChanged("disableWatcher", old.disableWatcher, new.disableWatcher)
      putChanged("podcastSearchRegion", old.podcastSearchRegion, new.podcastSearchRegion)
      putChangedNullable(
        "markAsFinishedPercentComplete",
        old.finishThreshold.serializedPercentComplete,
        new.finishThreshold.serializedPercentComplete,
      )
      putChangedNullable(
        "markAsFinishedTimeRemaining",
        old.finishThreshold.serializedTimeRemaining,
        new.finishThreshold.serializedTimeRemaining,
      )
    }
    putChangedNullable(
      "autoScanCronExpression",
      before.scheduleExpressionOrNull(),
      after.scheduleExpressionOrNull(),
    )
  }

private fun kotlinx.serialization.json.JsonObjectBuilder.putChanged(
  key: String,
  before: Boolean,
  after: Boolean,
) {
  if (before != after) put(key, after)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putChanged(
  key: String,
  before: Int,
  after: Int,
) {
  if (before != after) put(key, after)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putChanged(
  key: String,
  before: String,
  after: String,
) {
  if (before != after) put(key, after)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putChangedNullable(
  key: String,
  before: Int?,
  after: Int?,
) {
  if (before != after) put(key, after?.let(::JsonPrimitive) ?: JsonNull)
}

private fun kotlinx.serialization.json.JsonObjectBuilder.putChangedNullable(
  key: String,
  before: String?,
  after: String?,
) {
  if (before != after) put(key, after?.let(::JsonPrimitive) ?: JsonNull)
}
