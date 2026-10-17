package dev.halim.shelfdroid.widget.player

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException

suspend fun observePlayerWidgetUpdates(context: Context) {
  playerWidgetStore(context).uiState.playerWidgetData().collect {
    try {
      PlayerWidget().updateAll(context)
    } catch (exception: CancellationException) {
      throw exception
    } catch (exception: Exception) {
      Log.e("PlayerWidget", "Unable to update player widgets", exception)
    }
  }
}
