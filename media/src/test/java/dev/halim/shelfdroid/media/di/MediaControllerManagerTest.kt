package dev.halim.shelfdroid.media.di

import androidx.media3.common.Player
import com.google.common.util.concurrent.SettableFuture
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class MediaControllerManagerTest {
  @Test
  fun clear_and_stop_before_init_does_not_crash() {
    val manager = MediaControllerManager(SettableFuture.create())

    manager.clearAndStop()
  }

  @Test
  fun play_pause_pauses_when_playback_is_requested_but_buffering() {
    val player = mock(Player::class.java)
    `when`(player.isPlaying).thenReturn(false)
    `when`(player.playWhenReady).thenReturn(true)
    `when`(player.playbackState).thenReturn(Player.STATE_BUFFERING)

    player.togglePlayback()

    verify(player).pause()
    verify(player, never()).play()
  }
}
