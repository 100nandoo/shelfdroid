package dev.halim.shelfdroid.widget

import android.content.Intent
import dev.halim.shelfdroid.core.ui.screen.MainActivity
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_PLAYER
import dev.halim.shelfdroid.helper.Helper.Companion.EXTRA_MEDIA_ID
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class PlaybackWidgetNavigationTest {
  @Test
  fun currentPlaybackIntent_opensNowPlayingForCurrentMedia() {
    val intent = createNowPlayingIntent(RuntimeEnvironment.getApplication(), MEDIA_ID)

    assertEquals(MainActivity::class.java.name, intent.component?.className)
    assertEquals(ACTION_OPEN_PLAYER, intent.action)
    assertEquals(MEDIA_ID, intent.getStringExtra(EXTRA_MEDIA_ID))
    assertEquals(Intent.FLAG_ACTIVITY_SINGLE_TOP, intent.flags)
  }

  private companion object {
    const val MEDIA_ID = "book-id"
  }
}
