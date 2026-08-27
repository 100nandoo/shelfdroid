package dev.halim.shelfdroid.core.data.screen.libraryadmin

import dev.halim.shelfdroid.core.data.task.ServerTaskSocket
import dev.halim.socketio.SocketEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Narrow socket owner used by the application-scoped repository and deterministic integration
 * tests. It owns independent Library subscriptions but never replaces or closes other consumers'
 * subscriptions.
 */
internal class LibraryAdminEventOwner(
  private val socket: ServerTaskSocket,
  private val json: Json,
  private val scope: CoroutineScope,
  private val reconciler: LibraryAdminEventReconciler,
  private val publish: suspend (LibraryAdminLibraryEvent) -> Unit,
) : AutoCloseable {
  private val socketOwner = socket.acquire()
  private val subscriptions = mutableListOf<AutoCloseable>()

  init {
    listOf(
        SocketEvent.Library.Added,
        SocketEvent.Library.Updated,
        SocketEvent.Library.Removed,
      )
      .forEach { eventName ->
        subscriptions +=
          socket.subscribe(eventName) { args ->
            val event =
              parseLibraryAdminLibraryEvent(eventName, args, json) ?: return@subscribe
            if (reconciler.accept(event)) {
              scope.launch { reconcileAndPublish(event) }
            }
          }
      }

    // Socket.IO does not replay events missed during a disconnect. A connection callback triggers
    // one authoritative synchronization; there is intentionally no polling loop.
    subscriptions +=
      socket.subscribe(SocketEvent.Connect) {
        scope.launch {
          val refresh =
            LibraryAdminLibraryEvent(
              type = LibraryAdminLibraryEventType.REFRESHED,
            )
          if (reconciler.accept(refresh)) reconcileAndPublish(refresh)
        }
      }
  }

  private suspend fun reconcileAndPublish(event: LibraryAdminLibraryEvent) {
    publish(event)
  }

  override fun close() {
    subscriptions.forEach(AutoCloseable::close)
    subscriptions.clear()
    socketOwner.close()
  }
}
