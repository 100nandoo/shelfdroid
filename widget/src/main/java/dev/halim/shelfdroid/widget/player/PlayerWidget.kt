package dev.halim.shelfdroid.widget.player

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

class PlayerWidget : GlanceAppWidget() {
  override val sizeMode = SizeMode.Exact

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val playerState = playerWidgetStore(context).uiState
    val contentFlow = playerState.playerWidgetData()
    val colors = playerWidgetColorProviders(context)
    provideContent {
      GlanceTheme(colors = colors) {
        val content by
          contentFlow.collectAsState(playerState.collectAsState().value.toPlayerWidgetData())
        PlayerWidgetContent(content = content)
      }
    }
  }
}
