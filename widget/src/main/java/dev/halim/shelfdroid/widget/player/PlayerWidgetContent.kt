package dev.halim.shelfdroid.widget.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.components.PlayerWidgetControlButton
import dev.halim.shelfdroid.widget.player.components.PlayerWidgetCover

@Composable
internal fun PlayerWidgetContent(content: PlayerWidgetData?) {
  val context = LocalContext.current
  val size = LocalSize.current
  val coverSize = minOf(48.dp, (size.height - 8.dp).coerceAtLeast(1.dp))

  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .background(GlanceTheme.colors.surface)
        .cornerRadius(R.dimen.player_widget_background_radius)
        .padding(horizontal = 8.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      content
        ?.takeIf { size.width >= 280.dp }
        ?.let { current ->
          PlayerWidgetCover(cover = current.cover, size = coverSize)
          Spacer(GlanceModifier.width(8.dp))
        }
      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(
          text = content?.title ?: context.getString(R.string.player_widget_nothing_playing),
          style =
            TextStyle(
              color = GlanceTheme.colors.onSurface,
              fontFamily = FontFamily.Serif,
              fontSize = 16.sp,
              fontWeight = FontWeight.Medium,
            ),
          maxLines = if (size.height < 80.dp) 1 else 2,
        )
        content?.let { current ->
          Text(
            text = current.author,
            style =
              TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontFamily = FontFamily.SansSerif,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
              ),
            maxLines = 1,
          )
        }
      }
      content?.let { current ->
        Spacer(GlanceModifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
          PlayerWidgetControlButton(
            icon = CoreR.drawable.fast_rewind,
            contentDescription = context.getString(CoreR.string.seek_back),
            enabled = current.seekBackEnabled,
            tint = GlanceTheme.colors.onSurfaceVariant,
            action = widgetAction(WIDGET_ACTION_SEEK_BACK),
          )
          PlayerWidgetControlButton(
            icon = if (current.showPlayIcon) CoreR.drawable.play_arrow else CoreR.drawable.pause,
            contentDescription =
              context.getString(
                if (current.showPlayIcon) CoreR.string.play else CoreR.string.pause
              ),
            enabled = current.playPauseEnabled,
            tint = GlanceTheme.colors.onPrimary,
            action = widgetAction(WIDGET_ACTION_PLAY_PAUSE),
            background = GlanceTheme.colors.primary,
          )
          PlayerWidgetControlButton(
            icon = CoreR.drawable.fast_forward,
            contentDescription = context.getString(CoreR.string.seek_forward),
            enabled = current.seekForwardEnabled,
            tint = GlanceTheme.colors.onSurfaceVariant,
            action = widgetAction(WIDGET_ACTION_SEEK_FORWARD),
          )
        }
      }
    }
  }
}
