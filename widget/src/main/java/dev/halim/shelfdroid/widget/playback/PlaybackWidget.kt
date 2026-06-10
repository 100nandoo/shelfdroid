package dev.halim.shelfdroid.widget.playback

import android.content.Context
import android.content.Intent
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.components.FilledButton
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import dev.halim.shelfdroid.widget.R

class PlaybackWidget : GlanceAppWidget() {
  override val sizeMode: SizeMode = SizeMode.Single

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val openAppAction = actionStartActivity(openAppIntent(context))

    provideContent {
      Box(
        modifier = GlanceModifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = context.getString(R.string.playback_widget_empty_title))
          Spacer(modifier = GlanceModifier.height(12.dp))
          FilledButton(
            text = context.getString(R.string.playback_widget_open_app),
            onClick = openAppAction,
          )
        }
      }
    }
  }

  private fun openAppIntent(context: Context): Intent =
    Intent(Intent.ACTION_MAIN)
      .addCategory(Intent.CATEGORY_LAUNCHER)
      .setPackage(context.packageName)
      .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}
