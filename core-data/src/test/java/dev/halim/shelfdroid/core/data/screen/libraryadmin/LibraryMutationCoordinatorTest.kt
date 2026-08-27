package dev.halim.shelfdroid.core.data.screen.libraryadmin

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

  @Test
  fun reorderWaitsForCreateAndRunsAsOneGlobalMutation() = runTest {
    val coordinator = LibraryMutationCoordinator()
    val createEntered = CompletableDeferred<Unit>()
    val releaseCreate = CompletableDeferred<Unit>()
    val reorderEntered = CompletableDeferred<Unit>()
    val completedMutations = mutableListOf<String>()

    val create =
      async {
        coordinator.withMutation {
          createEntered.complete(Unit)
          releaseCreate.await()
          completedMutations += "create-library"
        }
      }
    createEntered.await()

    val reorder =
      async {
        coordinator.withMutation {
          reorderEntered.complete(Unit)
          completedMutations += "reorder-libraries"
        }
      }

    assertFalse(reorderEntered.isCompleted)
    releaseCreate.complete(Unit)
    create.await()
    reorder.await()

    assertEquals(listOf("create-library", "reorder-libraries"), completedMutations)
  }
}
