package dev.halim.shelfdroid.media.di

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@OptIn(ExperimentalCoroutinesApi::class)
class MediaControllerManagerTest {
  @Test
  fun reconnect_uses_a_fresh_controller_and_delivers_play_pause() = runTest {
    mockStatic(Log::class.java).use {
      mockStatic(MediaController::class.java).use { mediaControllerStatics ->
        val first = mock(MediaController::class.java)
        val second = mock(MediaController::class.java)
        `when`(first.isConnected).thenReturn(true)
        `when`(second.isConnected).thenReturn(true)
        `when`(second.playWhenReady).thenReturn(true)
        `when`(second.playbackState).thenReturn(Player.STATE_READY)
        val firstFuture = SettableFuture.create<MediaController>().apply { set(first) }
        val secondFuture = SettableFuture.create<MediaController>().apply { set(second) }
        val futures = ArrayDeque(listOf(firstFuture, secondFuture))
        val manager = MediaControllerManager { futures.removeFirst() }

        manager.init(this)
        runCurrent()
        manager.release()
        manager.init(this)
        runCurrent()
        manager.playPause()

        verify(second).pause()
        verify(first, never()).pause()
        mediaControllerStatics.verify { MediaController.releaseFuture(firstFuture) }
        assertEquals(0, futures.size)
        manager.release()
      }
    }
  }

  @Test
  fun release_while_connecting_ignores_late_result_and_allows_reconnection() = runTest {
    mockStatic(Log::class.java).use {
      mockStatic(MediaController::class.java).use { mediaControllerStatics ->
        val firstFuture = SettableFuture.create<MediaController>()
        val secondFuture = SettableFuture.create<MediaController>()
        val futures = ArrayDeque(listOf(firstFuture, secondFuture))
        val manager = MediaControllerManager { futures.removeFirst() }
        val first = mock(MediaController::class.java)
        val second = mock(MediaController::class.java)
        `when`(second.isConnected).thenReturn(true)
        `when`(second.playWhenReady).thenReturn(true)
        `when`(second.playbackState).thenReturn(Player.STATE_READY)

        manager.init(this)
        runCurrent()
        manager.playPause()
        manager.release()
        manager.init(this)
        runCurrent()
        secondFuture.set(second)
        runCurrent()
        firstFuture.set(first)
        runCurrent()
        manager.playPause()

        verify(second).pause()
        verify(first, never()).pause()
        mediaControllerStatics.verify { MediaController.releaseFuture(firstFuture) }
        manager.release()
      }
    }
  }

  @Test
  fun repeated_init_shares_the_pending_connection() = runTest {
    mockStatic(Log::class.java).use {
      mockStatic(MediaController::class.java).use {
        val future = SettableFuture.create<MediaController>()
        var connections = 0
        val manager = MediaControllerManager {
          connections++
          future
        }
        manager.init(this)
        manager.init(this)
        runCurrent()
        manager.init(this)

        assertEquals(1, connections)
        manager.release()
      }
    }
  }

  @Test
  fun play_pause_before_controller_connects_does_not_crash() {
    val manager = MediaControllerManager { SettableFuture.create() }

    manager.playPause()
  }

  @Test
  fun clear_and_stop_before_init_does_not_crash() {
    val manager = MediaControllerManager { SettableFuture.create() }

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
