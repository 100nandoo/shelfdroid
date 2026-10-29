package dev.halim.shelfdroid.widget.player.ui.components

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import dev.halim.shelfdroid.widget.R

@Composable
internal fun PlayerWidgetCover(
  cover: String,
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
    var bitmap by remember(cover, size) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(cover, size) {
      if (cover.isNotBlank()) {
        val imageSize =
          (size.value * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        val request =
          ImageRequest.Builder(context).data(cover).size(imageSize).allowHardware(false).build()
        bitmap = SingletonImageLoader.get(context).execute(request).image?.toBitmap()
      }
    }

    val image = bitmap
    if (image != null) {
      Image(
        provider = ImageProvider(image),
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
