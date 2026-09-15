package dev.halim.shelfdroid.media.di

import androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@UnstableApi
@RunWith(AndroidJUnit4::class)
class PlayerModuleTest {
  @Test
  fun providesPlayerAppliesAudioOffloadPreferences() {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext

    instrumentation.runOnMainSync {
      val player = PlayerModule.providesPlayer(context, DefaultMediaSourceFactory(context))
      try {
        val preferences = player.trackSelectionParameters.audioOffloadPreferences
        assertEquals(
          AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED,
          preferences.audioOffloadMode,
        )
        assertTrue(preferences.isGaplessSupportRequired)
        assertTrue(preferences.isSpeedChangeSupportRequired)
      } finally {
        player.release()
      }
    }
  }
}
