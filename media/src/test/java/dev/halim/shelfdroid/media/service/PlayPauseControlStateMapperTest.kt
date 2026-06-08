package dev.halim.shelfdroid.media.service

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayPauseControlStateMapperTest {
  private val mapper = PlayPauseControlStateMapper()

  @Test
  fun `map returns paused control state for ready paused media`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = false,
          playWhenReady = false,
          isLoading = false,
          playbackState = Player.STATE_READY,
          playPauseEnabled = true,
          showPlayIcon = true,
        )
      )

    assertFalse(state.isPlaying)
    assertTrue(state.enabled)
    assertTrue(state.showPlayIcon)
    assertFalse(state.showLoadingIndicator)
  }

  @Test
  fun `map returns active pause semantics for playing media`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = true,
          playWhenReady = true,
          isLoading = false,
          playbackState = Player.STATE_READY,
          playPauseEnabled = true,
          showPlayIcon = false,
        )
      )

    assertTrue(state.isPlaying)
    assertTrue(state.enabled)
    assertFalse(state.showPlayIcon)
    assertFalse(state.showLoadingIndicator)
  }

  @Test
  fun `map shows loading during initial prepare when playback should start`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = false,
          playWhenReady = true,
          isLoading = true,
          playbackState = Player.STATE_IDLE,
          playPauseEnabled = true,
          showPlayIcon = false,
        )
      )

    assertFalse(state.isPlaying)
    assertTrue(state.enabled)
    assertFalse(state.showPlayIcon)
    assertTrue(state.showLoadingIndicator)
  }

  @Test
  fun `map shows loading during buffering playback`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = false,
          playWhenReady = true,
          isLoading = false,
          playbackState = Player.STATE_BUFFERING,
          playPauseEnabled = true,
          showPlayIcon = false,
        )
      )

    assertTrue(state.enabled)
    assertFalse(state.showPlayIcon)
    assertTrue(state.showLoadingIndicator)
  }

  @Test
  fun `map does not show loading when paused media is still loading`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = false,
          playWhenReady = false,
          isLoading = true,
          playbackState = Player.STATE_BUFFERING,
          playPauseEnabled = true,
          showPlayIcon = true,
        )
      )

    assertTrue(state.showPlayIcon)
    assertFalse(state.showLoadingIndicator)
  }

  @Test
  fun `map preserves disabled ended control state`() {
    val state =
      mapper.map(
        snapshot(
          isPlaying = false,
          playWhenReady = false,
          isLoading = false,
          playbackState = Player.STATE_ENDED,
          playPauseEnabled = false,
          showPlayIcon = true,
        )
      )

    assertFalse(state.enabled)
    assertTrue(state.showPlayIcon)
    assertFalse(state.showLoadingIndicator)
    assertEquals(false, state.isPlaying)
  }

  private fun snapshot(
    isPlaying: Boolean,
    playWhenReady: Boolean,
    isLoading: Boolean,
    playbackState: Int,
    playPauseEnabled: Boolean,
    showPlayIcon: Boolean,
  ) =
    PlayerControlSnapshot(
      isPlaying = isPlaying,
      playWhenReady = playWhenReady,
      isLoading = isLoading,
      playbackState = playbackState,
      playPauseEnabled = playPauseEnabled,
      showPlayIcon = showPlayIcon,
    )
}
