package dev.halim.shelfdroid.widget.player.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.size
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.halim.shelfdroid.widget.R

@Composable
internal fun PlayerWidgetCover(
  bitmap: Bitmap?,
  size: Dp,
  previewCoverResource: Int? = null,
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
  if (previewCoverResource != null) {
    Image(
      provider = ImageProvider(previewCoverResource),
      contentDescription = context.getString(R.string.player_widget_cover_image),
      modifier = modifier,
      contentScale = ContentScale.Fit,
    )
  } else {
    if (bitmap != null) {
      Image(
        provider = ImageProvider(bitmap),
        contentDescription = context.getString(R.string.player_widget_cover_image),
        modifier = modifier,
        contentScale = ContentScale.Fit,
      )
    } else {
      Box(
        modifier = modifier.background(GlanceTheme.colors.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = context.getString(R.string.player_widget_no_cover),
          style =
            TextStyle(
              color = GlanceTheme.colors.onSecondaryContainer,
              fontSize = 10.sp,
              textAlign = TextAlign.Center,
            ),
        )
      }
    }
  }
}
