package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.core.network.response.Library
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.library.LibraryDataRepository
import dev.halim.shelfdroid.core.data.library.LibraryItemRepository
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.data.task.ServerTaskSocket
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json

/**
 * Application-scoped Library event owner. It shares SocketManager ownership with Server tasks and
 * podcast consumers, and keeps its subscriptions registered across socket reconnections.
 */
@Singleton
class LibraryAdminEventRepository
@Inject
constructor(
  socket: ServerTaskSocket,
  private val json: Json,
  @Named("io") private val scope: CoroutineScope,
  libraryDataRepository: LibraryDataRepository,
  libraryRepository: LibraryRepository,
  libraryItemRepository: LibraryItemRepository,
  mutationCoordinator: LibraryMutationCoordinator,
) {
  private val _events = MutableSharedFlow<LibraryAdminLibraryEvent>(extraBufferCapacity = 32)
  val events: SharedFlow<LibraryAdminLibraryEvent> = _events.asSharedFlow()

  /** Marks a successful in-app mutation so its server echo does not trigger a second sync. */
  internal fun registerLocalMutation(
    type: LibraryAdminLibraryEventType,
    library: Library,
  ) {
    val event =
      LibraryAdminLibraryEvent(
        type = type,
        library = library.toAdministrationLibrary(),
        fingerprint = json.encodeToString(library).hashCode(),
      )
    reconciler.registerLocalMutation(event)
  }

  private val reconciler =
    LibraryAdminEventReconciler(
      mutationCoordinator = mutationCoordinator,
      synchronize = {
        val result = libraryDataRepository.synchronize()
        if (result.isSuccess) {
          Result.success(Unit)
        } else {
          Result.failure(result.error ?: IllegalStateException("Library synchronization failed"))
        }
      },
      removeLibraryItems = libraryItemRepository::removeLibraryFromCatalog,
      removeLibrary = libraryRepository::removeFromCatalog,
      currentLibraries = {
        libraryRepository.listLibraries().map { entity ->
          LibraryAdminLibrary(
            id = entity.id,
            name = entity.name,
            mediaType =
              if (entity.isBookLibrary == 1L) {
                MediaType.BOOK
              } else {
                MediaType.PODCAST
              },
            displayOrder = entity.displayOrder.toInt(),
            icon = entity.icon,
          )
        }
      },
    )

  // Holding one owner is deliberate: this repository is app-scoped and its event subscriptions
  // must remain available while the administration screen is recreated or left in the back stack.
  private val eventOwner =
    LibraryAdminEventOwner(socket, json, scope, reconciler, ::reconcileAndPublish)

  private suspend fun reconcileAndPublish(event: LibraryAdminLibraryEvent) {
    reconciler
      .reconcile(event)
      .fold(
        onSuccess = { libraries ->
          _events.emit(event.copy(libraries = libraries, synchronized = true))
        },
        onFailure = {
          // Keep the event visible to the screen so it can show/retry the normal safe refresh
          // error;
          // internal server or transport details never cross this boundary.
          _events.emit(event.copy(libraries = emptyList(), synchronized = false))
        },
      )
  }
}
