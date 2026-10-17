package dev.halim.shelfdroid.widget.player

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetManager.Companion.SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

suspend fun observePlayerWidgetUpdates(context: Context) {
  val dataStoreManager = playerWidgetDataStoreManager(context)
  playerWidgetUpdateFlow(
      playbackData = playerWidgetStore(context).uiState.playerWidgetData(),
      darkMode = dataStoreManager.darkMode,
      dynamicTheme = dataStoreManager.dynamicTheme,
    )
    .collect {
      try {
        PlayerWidget().updateAll(context)
      } catch (exception: CancellationException) {
        throw exception
      } catch (exception: Exception) {
        Log.e("PlayerWidget", "Unable to update player widgets", exception)
      }
    }
}

suspend fun updatePlayerWidgetPreview(context: Context) {
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) return

  try {
    val result = GlanceAppWidgetManager(context).setWidgetPreviews(PlayerWidgetReceiver::class)
    if (result == SET_WIDGET_PREVIEWS_RESULT_RATE_LIMITED) {
      Log.i("PlayerWidget", "Preview update rate limited; it will retry on the next app launch")
    }
  } catch (exception: CancellationException) {
    throw exception
  } catch (exception: Exception) {
    Log.e("PlayerWidget", "Unable to update player widget preview", exception)
  }
}

internal fun playerWidgetUpdateFlow(
  playbackData: Flow<PlayerWidgetData?>,
  darkMode: Flow<Boolean>,
  dynamicTheme: Flow<Boolean>,
): Flow<Unit> = combine(playbackData, darkMode, dynamicTheme) { _, _, _ -> Unit }
