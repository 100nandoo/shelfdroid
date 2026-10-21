package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.test.core.app.ApplicationProvider
import dev.halim.shelfdroid.widget.player.playerWidgetPreviewData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerWidgetRenderingTest {
  @Test
  fun `preview remote views inflate with metadata at every widget size`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    for (height in listOf(96, 148, 320)) {
      val remoteViews =
        GlanceRemoteViews()
          .compose(context, DpSize(320.dp, height.dp)) {
            GlanceTheme { PlayerWidgetContent(playerWidgetPreviewData) }
          }
          .remoteViews
      val view = remoteViews.apply(context, FrameLayout(context))
      assertTrue("Missing author at height $height", view.containsAuthor())
    }
  }

  private fun View.containsAuthor(): Boolean =
    (this is TextView && text.toString() == playerWidgetPreviewData.author) ||
      (this is ViewGroup && (0 until childCount).any { getChildAt(it).containsAuthor() })
}
