package dev.halim.shelfdroid.widget.player

import android.content.Context
import android.util.Log
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

internal fun playerWidgetUpdateFlow(
  playbackData: Flow<PlayerWidgetData?>,
  darkMode: Flow<Boolean>,
  dynamicTheme: Flow<Boolean>,
): Flow<Unit> = combine(playbackData, darkMode, dynamicTheme) { _, _, _ -> Unit }
