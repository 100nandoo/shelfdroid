package dev.halim.shelfdroid.core.data.screen.libraryadministration

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryAdministrationDeleteMutationTest {

  @Test
  fun postAcceptanceLocalFailureIsClassifiedAsPartialSuccess() = runTest {
    val failure = IllegalStateException("catalog cleanup failed")

    val outcome = runAcceptedLibraryDeleteMutation { throw failure }

    assertTrue(outcome is LibraryAdministrationMutationResult.AcceptedButNotSynchronized)
    assertSame(
      failure,
      (outcome as LibraryAdministrationMutationResult.AcceptedButNotSynchronized).error,
    )
  }

  @Test
  fun cancellationIsPropagatedInsteadOfClassifiedAsPartialSuccess() = runTest {
    val cancellation = CancellationException("delete cancelled")

    try {
      runAcceptedLibraryDeleteMutation { throw cancellation }
    } catch (error: CancellationException) {
      assertSame(cancellation, error)
      return@runTest
    }

    throw AssertionError("CancellationException should be propagated")
  }
}
