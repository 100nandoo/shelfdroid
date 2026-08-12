package dev.halim.shelfdroid.core.data.sessionreset

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalSessionCleanupTest {

  @Test
  fun clear_clearsTransientDownloadsBeforePreferencesDatabaseAndAppStorage() = runTest {
    val events = mutableListOf<String>()
    val cleanup =
      LocalSessionCleanup(
        clearCurrentPlayback = { events += "current playback" },
        clearTransientDownloads = { events += "transient downloads" },
        resetLocalAppPreferences = { events += "local app preferences" },
        clearSessionScopedDatabase = { events += "database" },
        clearAppStorage = { events += "app storage" },
      )

    val result = cleanup.clear()

    assertTrue(result.isSuccess)
    assertEquals(
      listOf(
        "current playback",
        "transient downloads",
        "local app preferences",
        "database",
        "app storage",
      ),
      events,
    )
  }

  @Test
  fun clear_whenPreferenceResetFails_returnsFailureWithoutFurtherCleanup() = runTest {
    val failure = IllegalStateException("Preference reset failed")
    var databaseCleared = false
    var appStorageCleared = false
    val cleanup =
      LocalSessionCleanup(
        clearCurrentPlayback = {},
        clearTransientDownloads = {},
        resetLocalAppPreferences = { throw failure },
        clearSessionScopedDatabase = { databaseCleared = true },
        clearAppStorage = { appStorageCleared = true },
      )

    val result = cleanup.clear()

    assertTrue(result.isFailure)
    assertSame(failure, result.exceptionOrNull())
    assertTrue(!databaseCleared)
    assertTrue(!appStorageCleared)
  }

  @Test
  fun clear_whenDatabaseCleanupFails_returnsFailureWithoutClearingStorage() = runTest {
    val failure = IllegalStateException("Database cleanup failed")
    var appStorageCleared = false
    val cleanup =
      LocalSessionCleanup(
        clearCurrentPlayback = {},
        clearTransientDownloads = {},
        resetLocalAppPreferences = {},
        clearSessionScopedDatabase = { throw failure },
        clearAppStorage = { appStorageCleared = true },
      )

    val result = cleanup.clear()

    assertTrue(result.isFailure)
    assertSame(failure, result.exceptionOrNull())
    assertTrue(!appStorageCleared)
  }
}
