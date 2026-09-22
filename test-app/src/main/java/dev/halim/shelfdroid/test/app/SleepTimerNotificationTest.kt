package dev.halim.shelfdroid.test.app

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dev.halim.shelfdroid.core.PlayerTrack
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.prefs.NotificationPrefs
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.media.exoplayer.ExoPlayerManager
import dev.halim.shelfdroid.media.playback.PlayerStore
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@UnstableApi
@HiltAndroidTest
class SleepTimerNotificationTest {
  @get:Rule val hiltRule = HiltAndroidRule(this)

  @Inject lateinit var store: PlayerStore
  @Inject lateinit var dataStoreManager: DataStoreManager
  @Inject lateinit var playerManager: ExoPlayerManager
  private var audioFile: File? = null

  @Before
  fun setUp() {
    hiltRule.inject()
    updatePrefs(NotificationPrefs(sleepTimerMode = SleepTimerNotificationMode.Cyclical))
  }

  @After
  fun tearDown() {
    onMain { store.clearTimer() }
    runBlocking { dataStoreManager.clear() }
    onMain { playerManager.player.get().release() }
    audioFile?.delete()
  }

  @Test
  fun notificationTapsCycleOffOneFiveOffEvenWhenTappedImmediately() {
    onMain {
      store.sleepTimerFromMediaNotification()
      assertEquals(1.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(1.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  @Test
  fun cycleUsesOnlyEnabledPresetsInAscendingOrderAndAllowsOneDuration() {
    updatePrefs(
      NotificationPrefs(
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
        sleepTimerCycle = listOf(60, 5, 5, 2),
      )
    )
    onMain {
      store.sleepTimerFromMediaNotification()
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(60.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
    }

    updatePrefs(
      NotificationPrefs(
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
        sleepTimerCycle = listOf(30),
      )
    )
    onMain {
      store.sleepTimerFromMediaNotification()
      assertEquals(30.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  @Test
  fun timersStartedInPlayerAdvanceFromMatchingPresetOrCancelWhenUnmatched() {
    onMain {
      store.sleepTimer(1.minutes)
      store.sleepTimerFromMediaNotification()
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)

      store.sleepTimer(90.seconds)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(1.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  @Test
  fun changingModeOrEnabledPresetsLeavesTimerRunningUntilNextTap() {
    onMain { store.sleepTimerFromMediaNotification() }
    updatePrefs(
      NotificationPrefs(
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
        sleepTimerCycle = listOf(5, 15),
      )
    )
    onMain {
      assertEquals(1.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }

    updatePrefs(NotificationPrefs(sleepTimerMinutes = 30))
    onMain {
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(30.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }

    updatePrefs(
      NotificationPrefs(
        sleepTimerMode = SleepTimerNotificationMode.Cyclical,
        sleepTimerCycle = listOf(30, 60),
      )
    )
    onMain {
      assertEquals(30.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
      store.sleepTimerFromMediaNotification()
      assertEquals(60.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  @Test
  fun countdownAndPlaybackRestartKeepOriginalPresetForNextNotificationTap() {
    startSilentPlayback()
    onMain { store.sleepTimer(1.minutes) }
    runBlocking {
      withTimeout(5_000) {
        store.uiState.first {
          it.advancedControl.sleepTimerLeft > Duration.ZERO &&
            it.advancedControl.sleepTimerLeft < 1.minutes
        }
      }
    }
    onMain {
      playerManager.player.get().pause()
      store.changeContent()
      store.sleepTimerFromMediaNotification()
      assertEquals(5.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  @Test
  fun expiredTimerReturnsToOffAndNextTapStartsShortestPreset() {
    startSilentPlayback()
    onMain { store.sleepTimer(1.seconds) }
    runBlocking {
      withTimeout(5_000) {
        store.uiState.first { it.advancedControl.sleepTimerLeft == Duration.ZERO }
      }
    }
    waitForPlayback(isPlaying = false)
    onMain {
      assertFalse(playerManager.player.get().isPlaying)
      assertEquals(Duration.ZERO, store.uiState.value.advancedControl.sleepTimerDuration)
      store.sleepTimerFromMediaNotification()
      assertEquals(1.minutes, store.uiState.value.advancedControl.sleepTimerLeft)
    }
  }

  private fun startSilentPlayback() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val file = File.createTempFile("sleep-timer-", ".wav", context.cacheDir)
    audioFile = file
    // One second of mono PCM silence, looped so the timer uses real playback time.
    val audio = ByteBuffer.allocate(44 + 16_000).order(ByteOrder.LITTLE_ENDIAN)
    audio.put("RIFF".toByteArray()).putInt(36 + 16_000).put("WAVEfmt ".toByteArray())
    audio.putInt(16).putShort(1).putShort(1).putInt(8_000).putInt(16_000)
    audio.putShort(2).putShort(16).put("data".toByteArray()).putInt(16_000)
    file.writeBytes(audio.array())
    val uri = Uri.fromFile(file)
    onMain {
      store.uiState.value =
        store.uiState.value.copy(
          id = "timer-test",
          currentChapter = null,
          currentTrack = PlayerTrack(url = uri.toString()),
        )
      playerManager.player.get().apply {
        setMediaItem(MediaItem.fromUri(uri))
        repeatMode = Player.REPEAT_MODE_ONE
        prepare()
        play()
      }
    }
    waitForPlayback(isPlaying = true)
  }

  private fun waitForPlayback(isPlaying: Boolean) = runBlocking {
    withTimeout(5_000) {
      while (true) {
        var playing = false
        onMain { playing = playerManager.player.get().isPlaying }
        if (playing == isPlaying) break
        delay(25)
      }
    }
  }

  private fun updatePrefs(prefs: NotificationPrefs) = runBlocking {
    dataStoreManager.updateNotificationPrefs(prefs)
    withTimeout(5_000) { store.notificationPrefs.first { it == prefs } }
  }

  private fun onMain(block: () -> Unit) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
  }
}
