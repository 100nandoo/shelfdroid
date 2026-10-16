package dev.halim.shelfdroid.widget.player

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.components.PlayerWidgetCover

class PlayerWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val playerState = playerWidgetStore(context).uiState
    val contentFlow = playerState.widgetContent()
    provideContent {
      val content by contentFlow.collectAsState(playerState.value.toWidgetContent())
      val size = LocalSize.current
      val coverSize =
        minOf(
          48.dp,
          (size.height - 16.dp).coerceAtLeast(1.dp),
          ((size.width - 40.dp) / 2).coerceAtLeast(1.dp),
        )
      Box(
        modifier =
          GlanceModifier.fillMaxSize()
            .background(ColorProvider(Color(0xFFFFC981)))
            .cornerRadius(R.dimen.player_widget_background_radius)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          content?.let { current ->
            PlayerWidgetCover(cover = current.cover, size = coverSize)
            Spacer(GlanceModifier.width(8.dp))
          }
          Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
              text = content?.title ?: context.getString(R.string.player_widget_nothing_playing),
              style = TextStyle(color = ColorProvider(Color.White), fontSize = 16.sp),
              maxLines = if (size.height < 80.dp) 1 else 2,
            )
            content?.let { current ->
              Text(
                text = current.author,
                style = TextStyle(color = ColorProvider(Color.White), fontSize = 14.sp),
                maxLines = 1,
              )
            }
          }
        }
      }
    }
  }
}
