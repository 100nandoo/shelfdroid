package dev.halim.shelfdroid.widget.player

import android.content.Context
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.widget.player.PlayerWidgetReceiver
import dev.halim.shelfdroid.widget.player.ui.PlayerWidgetContent
import dev.halim.shelfdroid.widget.player.ui.playerWidgetColorProviders

class PlayerWidget
private constructor(private val pinRequestPreviewData: PlayerWidgetData?) : GlanceAppWidget() {
  constructor() : this(null)

  override val sizeMode = SizeMode.Exact
  override val previewSizeMode =
    SizeMode.Responsive(setOf(DpSize(220.dp, 60.dp), DpSize(320.dp, 96.dp)))

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val colors = playerWidgetColorProviders(context)
    pinRequestPreviewData?.let { previewData ->
      provideContent {
        GlanceTheme(colors = colors) { PlayerWidgetContent(content = previewData) }
      }
      return
    }

    val playerState = playerWidgetStore(context).uiState
    val contentFlow = playerState.playerWidgetData()
    provideContent {
      GlanceTheme(colors = colors) {
        val content by
          contentFlow.collectAsState(playerState.collectAsState().value.toPlayerWidgetData())
        PlayerWidgetContent(content = content)
      }
    }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val colors = playerWidgetColorProviders(context)
    provideContent {
      GlanceTheme(colors = colors) { PlayerWidgetContent(content = playerWidgetPreviewData) }
    }
  }

  companion object {
    internal fun forPinRequest(currentPlayback: PlayerUiState): PlayerWidget =
      PlayerWidget(currentPlayback.toPlayerWidgetData() ?: playerWidgetPreviewData)
  }
}

suspend fun requestPinPlayerWidget(context: Context, currentPlayback: PlayerUiState): Boolean =
  GlanceAppWidgetManager(context)
    .requestPinGlanceAppWidget(
      receiver = PlayerWidgetReceiver::class.java,
      preview = PlayerWidget.forPinRequest(currentPlayback),
      previewSize = DpSize(220.dp, 60.dp),
    )
