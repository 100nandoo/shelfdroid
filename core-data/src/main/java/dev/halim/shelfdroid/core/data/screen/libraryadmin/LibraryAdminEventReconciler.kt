package dev.halim.shelfdroid.core.data.screen.libraryadmin

import java.util.ArrayDeque

/**
 * Serializes external Library changes with create/delete/reorder mutations and remembers recent
 * event payloads so a local mutation echo or duplicate socket delivery causes one reconciliation.
 */
internal class LibraryAdminEventReconciler(
  private val mutationCoordinator: LibraryMutationCoordinator,
  private val synchronize: suspend () -> Result<Unit>,
  private val removeLibraryItems: suspend (String) -> Unit,
  private val removeLibrary: (String) -> Unit,
  private val currentLibraries: () -> List<LibraryAdminLibrary>,
) {
  private val lock = Any()
  private val recentEvents = ArrayDeque<String>()
  private val recentEventKeys = mutableSetOf<String>()
  private val localMutationEchoes = LinkedHashMap<String, Long>()

  fun registerLocalMutation(event: LibraryAdminLibraryEvent) {
    if (event.type == LibraryAdminLibraryEventType.REFRESHED) return
    synchronized(lock) {
      pruneLocalMutationEchoesLocked()
      localMutationEchoes[event.deduplicationKey()] = System.nanoTime() + LOCAL_ECHO_RETENTION_NANOS
      while (localMutationEchoes.size > MAX_LOCAL_MUTATION_ECHOES) {
        localMutationEchoes.remove(localMutationEchoes.keys.first())
      }
    }
  }

  fun accept(event: LibraryAdminLibraryEvent): Boolean {
    // Each reconnect is a recovery boundary and must force a fresh authoritative snapshot.
    if (event.type == LibraryAdminLibraryEventType.REFRESHED) return true
    val key = event.deduplicationKey()
    synchronized(lock) {
      pruneLocalMutationEchoesLocked()
      if (localMutationEchoes.remove(key) != null) return false
      if (!recentEventKeys.add(key)) return false
      recentEvents.addLast(key)
      while (recentEvents.size > MAX_RECENT_EVENTS) {
        recentEventKeys.remove(recentEvents.removeFirst())
      }
      return true
    }
  }

  suspend fun reconcile(event: LibraryAdminLibraryEvent): Result<List<LibraryAdminLibrary>> =
    mutationCoordinator.withMutation {
      val eventKey = event.deduplicationKey()
      synchronized(lock) {
        pruneLocalMutationEchoesLocked()
        if (localMutationEchoes.remove(eventKey) != null) {
          return@withMutation Result.success(currentLibraries())
        }
      }
      if (event.type == LibraryAdminLibraryEventType.REMOVED) {
        val libraryId = event.library?.id ?: return@withMutation Result.failure(InvalidEvent)
        // Remove the local projection immediately. The subsequent server snapshot may fail, but
        // a confirmed server removal must never leave stale Library items in the Catalog.
        removeLibraryItems(libraryId)
        removeLibrary(libraryId)
      }

      synchronize()
        .fold(
          onSuccess = { Result.success(currentLibraries()) },
          onFailure = { Result.failure(it) },
        )
    }

  private companion object {
    const val MAX_RECENT_EVENTS = 128
    const val MAX_LOCAL_MUTATION_ECHOES = 32
    const val LOCAL_ECHO_RETENTION_NANOS = 60_000_000_000L
    val InvalidEvent = IllegalArgumentException("Library event has no Library id")
  }

  private fun pruneLocalMutationEchoesLocked() {
    val now = System.nanoTime()
    localMutationEchoes.entries.removeIf { (_, expiresAt) -> expiresAt <= now }
  }
}
