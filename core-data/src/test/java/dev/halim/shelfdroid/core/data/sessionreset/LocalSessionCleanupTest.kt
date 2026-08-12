package dev.halim.shelfdroid.core.data.sessionreset

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSessionCleanupTest {

  @Test
  fun clear_runsDatabaseAndAppStorageCleanupInOrder() = runTest {
    val events = mutableListOf<String>()
    val cleanup =
      LocalSessionCleanup(
        clearDatabase = { events += "database" },
        clearAppStorage = { events += "app storage" },
      )

    val result = cleanup.clear()

    assertTrue(result.isSuccess)
    assertEquals(listOf("database", "app storage"), events)
  }

  @Test
  fun clear_whenDatabaseCleanupFails_returnsFailureWithoutClearingStorage() = runTest {
    val failure = IllegalStateException("Database cleanup failed")
    var appStorageCleared = false
    val cleanup =
      LocalSessionCleanup(
        clearDatabase = { throw failure },
        clearAppStorage = { appStorageCleared = true },
      )

    val result = cleanup.clear()

    assertTrue(result.isFailure)
    assertSame(failure, result.exceptionOrNull())
    assertTrue(!appStorageCleared)
  }
}
