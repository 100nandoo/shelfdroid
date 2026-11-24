package dev.halim.shelfdroid.widget.player.ui

import android.app.Application
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.util.SizeF
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import android.widget.TextView
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.composeForPreview
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.ui.components.loadCoverPaddingSwatch
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidget
import dev.halim.shelfdroid.widget.player.PlayerWidgetEntryPoint
import dev.halim.shelfdroid.widget.player.WidgetCoverArtwork
import dev.halim.shelfdroid.widget.player.playerWidgetPreviewData
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@OptIn(ExperimentalGlanceRemoteViewsApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerWidgetRenderingTest {
  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun `rectangular widget covers tint the padding around the artwork`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val sidePx = (80 * context.resources.displayMetrics.density).roundToInt()
    val center = sidePx / 2
    val edge = sidePx / 10
    for ((width, height) in listOf(40 to 80, 80 to 40)) {
      val artwork = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
      artwork.eraseColor(Color.RED)
      val cover = WidgetCoverArtwork(artwork, loadCoverPaddingSwatch(artwork))
      val remoteViews =
        GlanceRemoteViews()
          .compose(context, DpSize(80.dp, 80.dp)) {
            GlanceTheme { PlayerWidgetCover(cover, resource = null, size = 80.dp) }
          }
          .remoteViews
      val view = remoteViews.apply(context, FrameLayout(context))
      val measureSpec = View.MeasureSpec.makeMeasureSpec(sidePx, View.MeasureSpec.EXACTLY)
      view.measure(measureSpec, measureSpec)
      view.layout(0, 0, sidePx, sidePx)
      val rendered = Bitmap.createBitmap(sidePx, sidePx, Bitmap.Config.ARGB_8888)
      view.draw(Canvas(rendered))

      val padding =
        if (width < height) rendered.getPixel(edge, center) else rendered.getPixel(center, edge)
      assertTrue("Expected a red tint in the padding", Color.red(padding) > Color.green(padding))
      assertEquals(Color.RED, rendered.getPixel(center, center))
    }
  }

  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun `padding swatches use visible pixels and skip fully transparent artwork`() = runTest {
    assertNull(loadCoverPaddingSwatch(Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888)))
    val artwork = Bitmap.createBitmap(40, 80, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply { color = Color.RED }
    Canvas(artwork).drawRect(0f, 38f, 40f, 42f, paint)
    val tint = checkNotNull(loadCoverPaddingSwatch(artwork)).blendWith(ComposeColor.Black)

    assertTrue(tint.red > tint.green)
  }

  @Test
  @Config(application = WidgetPreviewApplication::class)
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun `generated previews include metadata and decoded artwork in wide sizes`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val preview =
      PlayerWidget().composeForPreview(context, AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN)
    val coverDescription = context.getString(R.string.player_widget_cover_image)
    for (size in listOf(SizeF(220f, 60f), SizeF(320f, 128f), SizeF(320f, 180f))) {
      val layout =
        ReflectionHelpers.callInstanceMethod<RemoteViews>(
          preview,
          "getRemoteViewsToApply",
          ClassParameter.from(Context::class.java, context),
          ClassParameter.from(SizeF::class.java, size),
        )
      val view = layout.apply(context, FrameLayout(context))
      assertTrue("Missing author at $size", view.containsAuthor())
      if (size.width == 320f) {
        val images = arrayListOf<View>()
        view.findViewsWithText(images, coverDescription, View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION)
        val cover = images.filterIsInstance<ImageView>().single().drawable as BitmapDrawable
        assertTrue(cover.bitmap.height > cover.bitmap.width)
        val maxSidePx = (180 * context.resources.displayMetrics.density).toInt()
        assertTrue("Expected downsampled artwork at $size", cover.bitmap.height <= maxSidePx)
      }
    }
  }

  private fun View.containsAuthor(): Boolean =
    (this is TextView && text.toString() == playerWidgetPreviewData.author) ||
      (this is ViewGroup && (0 until childCount).any { getChildAt(it).containsAuthor() })
}

internal class WidgetPreviewApplication :
  Application(), GeneratedComponentManager<PlayerWidgetEntryPoint> {
  override fun generatedComponent(): PlayerWidgetEntryPoint =
    object : GeneratedComponent, PlayerWidgetEntryPoint {
      override fun dataStoreManager(): DataStoreManager =
        DataStoreManager(
          object : DataStore<Preferences> {
            override val data = flowOf(emptyPreferences())

            override suspend fun updateData(
              transform: suspend (Preferences) -> Preferences
            ): Preferences = error("The preview only reads preferences")
          }
        )

      override fun playerStore() = error("The generated preview is independent of playback")
    }
}
