package dev.halim.shelfdroid.widget.player.actions

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.helper.Helper

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
