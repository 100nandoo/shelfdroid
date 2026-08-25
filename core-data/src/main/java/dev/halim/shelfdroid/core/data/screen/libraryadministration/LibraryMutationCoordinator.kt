package dev.halim.shelfdroid.core.data.screen.libraryadministration

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Application-scoped serialization point for create, delete, and reorder mutations. */
@Singleton
class LibraryMutationCoordinator @Inject constructor() {
  private val mutex = Mutex()

  suspend fun <T> withMutation(block: suspend () -> T): T = mutex.withLock { block() }
}
