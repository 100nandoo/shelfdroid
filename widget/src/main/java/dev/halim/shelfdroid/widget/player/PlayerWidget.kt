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
import dev.halim.shelfdroid.widget.player.ui.PlayerWidgetContent
import dev.halim.shelfdroid.widget.player.ui.playerWidgetColorProviders
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first

class PlayerWidget private constructor(private val pinRequestPreviewData: PlayerWidgetData?) :
  GlanceAppWidget() {
  constructor() : this(null)

  override val sizeMode = SizeMode.Exact
  override val previewSizeMode =
    SizeMode.Responsive(
      setOf(DpSize(220.dp, 60.dp), DpSize(320.dp, 128.dp), DpSize(320.dp, 180.dp))
    )

  override suspend fun provideGlance(context: Context, id: GlanceId) = coroutineScope {
    val colors = playerWidgetColorProviders(context)
    val initialColors = colors.first()
    val memoryCapBytes = widgetMemoryCapBytes(context)
    pinRequestPreviewData?.let { previewData ->
      val targetSize =
        widgetCoverTargetSize(
          listOf(DpSize(220.dp, 60.dp)),
          context.resources.displayMetrics.density,
          memoryCapBytes,
        )
      val preview =
        previewData.copy(
          coverBitmap =
            previewData.cover
              .takeIf { it.isNotBlank() }
              ?.let {
                loadPlayerWidgetCover(context, it, targetSize, memoryCapBytes)
              }
        )
      provideContent {
        val currentColors by colors.collectAsState(initialColors)
        GlanceTheme(colors = currentColors) { PlayerWidgetContent(content = preview) }
      }
      return@coroutineScope
    }

    val sizes =
      GlanceAppWidgetManager(context)
        .getAppWidgetSizes(id)
        .filter { it.width > 0.dp && it.height > 0.dp }
        .ifEmpty { listOf(DpSize(220.dp, 60.dp)) }
    val targetSize =
      widgetCoverTargetSize(sizes, context.resources.displayMetrics.density, memoryCapBytes)
    val contentFlow =
      playerWidgetContentFlow(this, playerWidgetStore(context).uiState.playerWidgetData()) {
        loadPlayerWidgetCover(context, it, targetSize, memoryCapBytes)
      }
    val initialContent = contentFlow.first()
    provideContent {
      val currentColors by colors.collectAsState(initialColors)
      GlanceTheme(colors = currentColors) {
        val content by contentFlow.collectAsState(initialContent)
        PlayerWidgetContent(content = content)
      }
    }
  }

  override suspend fun providePreview(context: Context, widgetCategory: Int) {
    val colors = playerWidgetColorProviders(context).first()
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
