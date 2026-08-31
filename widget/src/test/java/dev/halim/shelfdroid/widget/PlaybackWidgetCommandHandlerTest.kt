package dev.halim.shelfdroid.widget

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWidgetCommandHandlerTest {
  @Test
  fun eachPrimaryAction_dispatchesItsStandardPlayerCommandAndRefreshes() {
    PrimaryPlaybackCommand.entries.forEach { command ->
      val gateway = FakePlaybackControllerGateway(PlaybackCommandResult.Success)
      val refreshRequester = FakeRefreshRequester()
      val navigator = FakeNormalAppNavigator()

      runTest { handlePlaybackWidgetCommand(command, gateway, refreshRequester, navigator) }

      assertEquals(listOf(command), gateway.commands)
      assertTrue(refreshRequester.wasRequested)
      assertFalse(navigator.wasOpened)
    }
  }

  @Test
  fun missingCurrentPlayback_opensNormalAppWithoutRefresh() = runTest {
    val gateway = FakePlaybackControllerGateway(PlaybackCommandResult.MissingCurrentPlayback)
    val refreshRequester = FakeRefreshRequester()
    val navigator = FakeNormalAppNavigator()

    handlePlaybackWidgetCommand(
      PrimaryPlaybackCommand.Play,
      gateway,
      refreshRequester,
      navigator,
    )

    assertFalse(refreshRequester.wasRequested)
    assertTrue(navigator.wasOpened)
  }

  @Test
  fun failedCommand_doesNotClaimSuccessRefreshOrOpenStalePlayback() = runTest {
    val gateway = FakePlaybackControllerGateway(PlaybackCommandResult.Failed)
    val refreshRequester = FakeRefreshRequester()
    val navigator = FakeNormalAppNavigator()

    handlePlaybackWidgetCommand(
      PrimaryPlaybackCommand.SeekForward,
      gateway,
      refreshRequester,
      navigator,
    )

    assertFalse(refreshRequester.wasRequested)
    assertFalse(navigator.wasOpened)
  }

  private class FakePlaybackControllerGateway(
    private val result: PlaybackCommandResult,
  ) : PlaybackControllerGateway {
    val commands = mutableListOf<PrimaryPlaybackCommand>()

    override suspend fun dispatch(command: PrimaryPlaybackCommand): PlaybackCommandResult {
      commands += command
      return result
    }
  }

  private class FakeRefreshRequester : PlaybackWidgetRefreshRequester {
    var wasRequested = false

    override suspend fun requestRefresh() {
      wasRequested = true
    }
  }

  private class FakeNormalAppNavigator : NormalAppNavigator {
    var wasOpened = false

    override fun openNormalApp() {
      wasOpened = true
    }
  }
}
