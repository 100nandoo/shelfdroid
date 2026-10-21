package dev.halim.shelfdroid.widget.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.helper.Helper
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

internal fun playerWidgetOpenAction(context: Context, mediaId: String): Action {
  val intent =
    Intent(Helper.ACTION_OPEN_PLAYER).apply {
      component = widgetMainActivityComponent(context)
      putExtra(Helper.EXTRA_MEDIA_ID, mediaId)
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_OPENING_SCREEN,
        MediaNotificationOpeningScreen.Home.name,
      )
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_PLAYER_PRESENTATION,
        MediaNotificationPlayerPresentation.ExpandedPlayer.name,
      )
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
  return actionStartActivity(intent)
}

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
