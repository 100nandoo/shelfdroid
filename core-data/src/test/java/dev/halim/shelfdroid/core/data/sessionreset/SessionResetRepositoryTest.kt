package dev.halim.shelfdroid.core.data.sessionreset

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionResetRepositoryTest {

  @Test
  fun fullLogout_withoutRefreshToken_failsWithoutLocalCleanup() = runTest {
    var localCleanupRan = false
    val repository =
      SessionResetRepository(
        refreshToken = { "" },
        remoteLogout = { Result.success(Unit) },
        localCleanup = {
          localCleanupRan = true
          Result.success(Unit)
        },
      )

    val result = repository.fullLogout()

    assertTrue(result.isFailure)
    assertFalse(localCleanupRan)
  }

  @Test
  fun fullLogout_whenRemoteLogoutFails_returnsFailureWithoutLocalCleanup() = runTest {
    val remoteFailure = IllegalStateException("Remote logout failed")
    var localCleanupRan = false
    val repository =
      SessionResetRepository(
        refreshToken = { "refresh-token" },
        remoteLogout = { Result.failure(remoteFailure) },
        localCleanup = {
          localCleanupRan = true
          Result.success(Unit)
        },
      )

    val result = repository.fullLogout()

    assertTrue(result.isFailure)
    assertSame(remoteFailure, result.exceptionOrNull())
    assertFalse(localCleanupRan)
  }

  @Test
  fun fullLogout_afterRemoteLogoutSucceeds_runsOrderedLocalCleanup() = runTest {
    val events = mutableListOf<String>()
    val localCleanup =
      LocalSessionCleanup(
        clearCurrentPlayback = { events += "current playback" },
        clearTransientDownloads = { events += "transient downloads" },
        resetLocalAppPreferences = { events += "local app preferences" },
        clearDatabase = { events += "database" },
        clearAppStorage = { events += "app storage" },
      )
    val repository =
      SessionResetRepository(
        refreshToken = { "refresh-token" },
        remoteLogout = {
          events += "remote logout"
          Result.success(Unit)
        },
        localCleanup = localCleanup::clear,
      )

    val result = repository.fullLogout()

    assertTrue(result.isSuccess)
    assertEquals(
      listOf(
        "remote logout",
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
  fun logoutForAccountSwitch_withoutRefreshToken_clearsPlaybackAndPreferences() = runTest {
    var remoteLogoutRan = false
    var currentPlaybackCleared = false
    var localAppPreferencesReset = false
    val localCleanup =
      LocalSessionCleanup(
        clearCurrentPlayback = { currentPlaybackCleared = true },
        clearTransientDownloads = {},
        resetLocalAppPreferences = { localAppPreferencesReset = true },
        clearDatabase = {},
        clearAppStorage = {},
      )
    val repository =
      SessionResetRepository(
        refreshToken = { "" },
        remoteLogout = {
          remoteLogoutRan = true
          Result.success(Unit)
        },
        localCleanup = localCleanup::clear,
      )

    val result = repository.logoutForAccountSwitch()

    assertTrue(result.isSuccess)
    assertFalse(remoteLogoutRan)
    assertTrue(currentPlaybackCleared)
    assertTrue(localAppPreferencesReset)
  }

  @Test
  fun logoutForAccountSwitch_withRefreshToken_attemptsRemoteLogoutBeforeLocalCleanup() = runTest {
    val events = mutableListOf<String>()
    val repository =
      SessionResetRepository(
        refreshToken = { "refresh-token" },
        remoteLogout = { token ->
          events += "remote logout with $token"
          Result.failure(IllegalStateException("Remote logout failed"))
        },
        localCleanup = {
          events += "local cleanup"
          Result.success(Unit)
        },
      )

    val result = repository.logoutForAccountSwitch()

    assertTrue(result.isSuccess)
    assertEquals(listOf("remote logout with refresh-token", "local cleanup"), events)
  }
}
