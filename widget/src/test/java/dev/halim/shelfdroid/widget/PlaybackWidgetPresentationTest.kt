package dev.halim.shelfdroid.widget

import android.net.Uri
import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackWidgetPresentationTest {
  @Test
  fun missingSessionOrCurrentItem_mapsToEmptyState() {
    assertEquals(
      PlaybackWidgetPresentation.Empty,
      mapPlaybackWidgetPresentation(snapshot = null, artwork = null),
    )
    assertEquals(
      PlaybackWidgetPresentation.Empty,
      mapPlaybackWidgetPresentation(
        snapshot =
          PlaybackSessionSnapshot(
            item = null,
            playbackState = Player.STATE_IDLE,
            playWhenReady = false,
            hasError = false,
          ),
        artwork = null,
      ),
    )
  }

  @Test
  fun loadedPlayingPausedBufferingAndEndedItems_mapToActiveState() {
    val playbackStates =
      listOf(
        Player.STATE_READY to true,
        Player.STATE_READY to false,
        Player.STATE_BUFFERING to true,
        Player.STATE_ENDED to false,
      )

    playbackStates.forEach { (playbackState, playWhenReady) ->
      val presentation =
        mapPlaybackWidgetPresentation(
          snapshot = snapshot(playbackState, playWhenReady),
          artwork = null,
        )

      assertTrue(presentation is PlaybackWidgetPresentation.Active)
    }
  }

  @Test
  fun playbackIntent_mapsPauseForPlayingAndBufferingAndPlayForPausedAndEnded() {
    assertTrue(activeControls(Player.STATE_READY, playWhenReady = true).showPause)
    assertTrue(activeControls(Player.STATE_BUFFERING, playWhenReady = true).showPause)
    assertFalse(activeControls(Player.STATE_BUFFERING, playWhenReady = false).showPause)
    assertFalse(activeControls(Player.STATE_READY, playWhenReady = false).showPause)
    assertFalse(activeControls(Player.STATE_ENDED, playWhenReady = true).showPause)
  }

  @Test
  fun commandAvailability_mapsToStableControlState() {
    val presentation =
      mapPlaybackWidgetPresentation(
        snapshot =
          snapshot(Player.STATE_READY, playWhenReady = true).copy(
            playPauseEnabled = false,
            seekBackEnabled = false,
            seekForwardEnabled = true,
          ),
        artwork = null,
      ) as PlaybackWidgetPresentation.Active

    assertEquals(
      PrimaryPlaybackControls(
        showPause = true,
        playPauseEnabled = false,
        seekBackEnabled = false,
        seekForwardEnabled = true,
      ),
      presentation.controls,
    )
  }

  @Test
  fun playbackError_retainsCurrentPlaybackIdentity() {
    val presentation =
      mapPlaybackWidgetPresentation(
        snapshot = snapshot(Player.STATE_IDLE, playWhenReady = false, hasError = true),
        artwork = null,
      )

    assertEquals(
      PlaybackWidgetPresentation.Error(
        CurrentPlaybackMedia(
          mediaId = MEDIA_ID,
          mediaTitle = MEDIA_TITLE,
          playableTitle = PLAYABLE_TITLE,
          artwork = null,
        )
      ),
      presentation,
    )
  }

  private fun snapshot(
    playbackState: Int,
    playWhenReady: Boolean,
    hasError: Boolean = false,
  ) =
    PlaybackSessionSnapshot(
      item =
        PlaybackSessionItem(
          mediaId = MEDIA_ID,
          mediaTitle = MEDIA_TITLE,
          playableTitle = PLAYABLE_TITLE,
          artworkData = null,
          artworkUri = Uri.EMPTY,
        ),
      playbackState = playbackState,
      playWhenReady = playWhenReady,
      hasError = hasError,
    )

  private fun activeControls(playbackState: Int, playWhenReady: Boolean) =
    (
        mapPlaybackWidgetPresentation(
          snapshot = snapshot(playbackState, playWhenReady),
          artwork = null,
        ) as PlaybackWidgetPresentation.Active
      )
      .controls

  private companion object {
    const val MEDIA_ID = "book-id"
    const val MEDIA_TITLE = "The Left Hand of Darkness"
    const val PLAYABLE_TITLE = "Chapter 1"
  }
}
