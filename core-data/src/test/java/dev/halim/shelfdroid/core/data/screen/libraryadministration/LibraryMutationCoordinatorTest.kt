package dev.halim.shelfdroid.core.data.screen.libraryadministration

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LibraryMutationCoordinatorTest {

  @Test
  fun createDeleteOrReorderMutations_areSerialized() = runTest {
    val coordinator = LibraryMutationCoordinator()
    val firstEntered = CompletableDeferred<Unit>()
    val releaseFirst = CompletableDeferred<Unit>()
    val secondEntered = CompletableDeferred<Unit>()
    val completedMutations = mutableListOf<String>()

    val create =
      async {
        coordinator.withMutation {
          firstEntered.complete(Unit)
          releaseFirst.await()
          completedMutations += "create"
        }
      }
    firstEntered.await()

    val delete =
      async {
        coordinator.withMutation {
          secondEntered.complete(Unit)
          completedMutations += "delete"
        }
      }

    assertFalse(secondEntered.isCompleted)
    releaseFirst.complete(Unit)
    create.await()
    delete.await()

    assertEquals(listOf("create", "delete"), completedMutations)
  }
}
