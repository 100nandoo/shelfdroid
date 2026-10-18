package dev.halim.shelfdroid.widget.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val WIDGET_ACTION_SEEK_BACK = "seek_back"
internal const val WIDGET_ACTION_PLAY_PAUSE = "play_pause"
internal const val WIDGET_ACTION_SEEK_FORWARD = "seek_forward"
internal const val WIDGET_ACTION_PREVIOUS_CHAPTER = "previous_chapter"
internal const val WIDGET_ACTION_NEXT_CHAPTER = "next_chapter"

private val widgetActionKey = ActionParameters.Key<String>("playerWidgetAction")

internal fun widgetMainActivityComponent(context: Context) =
  ComponentName(context.packageName, "dev.halim.shelfdroid.core.ui.screen.MainActivity")

internal fun widgetLaunchAction(context: Context) =
  actionStartActivity(Intent.makeMainActivity(widgetMainActivityComponent(context)))

internal fun widgetAction(action: String) =
  actionRunCallback<WidgetControlAction>(actionParametersOf(widgetActionKey to action))

internal class WidgetControlAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val action = parameters[widgetActionKey] ?: return
    withContext(Dispatchers.Main.immediate) {
      val playerStore = playerWidgetStore(context)
      when (action) {
        WIDGET_ACTION_SEEK_BACK -> playerStore.seekBack()
        WIDGET_ACTION_PLAY_PAUSE -> playerStore.playPause()
        WIDGET_ACTION_SEEK_FORWARD -> playerStore.seekForward()
        WIDGET_ACTION_PREVIOUS_CHAPTER -> playerStore.previousChapterFromWidget()
        WIDGET_ACTION_NEXT_CHAPTER -> playerStore.nextChapterFromWidget()
      }
    }
    PlayerWidget().update(context, glanceId)
  }
}

internal class DisabledWidgetControlAction : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) = Unit
}
