package dev.halim.shelfdroid.core.data.screen.player

import dev.halim.shelfdroid.core.DownloadState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackSessionResolverTest {

  private val resolver = PlaybackSessionResolver()

  @Test
  fun resolve_whenDownloadIsRecovered_usesLocalSessionWithoutCallingRemote() = runTest {
    var remoteCalls = 0

    val sessionId =
      resolver.resolve(DownloadState.Completed) {
        remoteCalls += 1
        "remote-session"
      }

    assertEquals(0, remoteCalls)
    assertTrue(sessionId.isNotBlank())
    assertTrue(sessionId != "remote-session")
  }

  @Test
  fun resolve_whenDownloadIsMissing_requestsRemoteSession() = runTest {
    var remoteCalls = 0

    val sessionId =
      resolver.resolve(DownloadState.Unknown) {
        remoteCalls += 1
        "remote-session"
      }

    assertEquals(1, remoteCalls)
    assertEquals("remote-session", sessionId)
  }
}
