package dev.halim.shelfdroid.widget.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.WidgetCoverArtwork

@Composable
internal fun PlayerWidgetCover(
  artwork: WidgetCoverArtwork?,
  resource: Int?,
  size: Dp,
  action: Action? = null,
) {
  val context = LocalContext.current
  val coverModifier =
    if (LocalSize.current.height < 116.dp) {
      GlanceModifier.size(size).cornerRadius(4.dp)
    } else {
      GlanceModifier.size(size).cornerRadius(8.dp)
    }
  val modifier = action?.let(coverModifier::clickable) ?: coverModifier
  val bitmap = artwork?.bitmap
  val provider = bitmap?.let { ImageProvider(it) } ?: resource?.let { ImageProvider(it) }
  val swatch = artwork?.paddingSwatch
  Box(
    modifier =
      if (provider == null) modifier.background(GlanceTheme.colors.secondaryContainer)
      else modifier,
    contentAlignment = Alignment.Center,
  ) {
    if (provider == null) {
      Text(
        text = context.getString(R.string.player_widget_no_cover),
        style =
          TextStyle(
            color = GlanceTheme.colors.onSecondaryContainer,
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
          ),
      )
    } else {
      if (bitmap != null && swatch != null) {
        val background = GlanceTheme.colors.secondaryContainer.getColor(context)
        val artworkRatio =
          minOf(bitmap.width, bitmap.height).toFloat() / maxOf(bitmap.width, bitmap.height)
        WidgetCoverPadding(
          size = size * (1f - artworkRatio) / 2f,
          portrait = bitmap.width < bitmap.height,
          color = swatch.blendWith(background),
        )
      }
      Image(
        provider = provider,
        contentDescription = context.getString(R.string.player_widget_cover_image),
        modifier = GlanceModifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
      )
    }
  }
}

@Composable
private fun WidgetCoverPadding(size: Dp, portrait: Boolean, color: Color) {
  if (portrait) {
    Row(GlanceModifier.fillMaxSize()) {
      val strip = GlanceModifier.width(size).fillMaxHeight().background(color)
      Box(strip) {}
      Spacer(GlanceModifier.defaultWeight())
      Box(strip) {}
    }
  } else {
    Column(GlanceModifier.fillMaxSize()) {
      val strip = GlanceModifier.height(size).fillMaxWidth().background(color)
      Box(strip) {}
      Spacer(GlanceModifier.defaultWeight())
      Box(strip) {}
    }
  }
}
