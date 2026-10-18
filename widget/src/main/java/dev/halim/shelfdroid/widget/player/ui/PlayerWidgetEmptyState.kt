package dev.halim.shelfdroid.widget.player.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.halim.shelfdroid.widget.R

@Composable
internal fun PlayerWidgetEmptyState() {
  val context = LocalContext.current
  val size = LocalSize.current
  val launchIntent =
    Intent(Intent.ACTION_MAIN).apply {
      setPackage(context.packageName)
      addCategory(Intent.CATEGORY_LAUNCHER)
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
  val action = actionStartActivity(launchIntent)

  Column(
    modifier = GlanceModifier.fillMaxSize(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = context.getString(R.string.player_widget_choose_something_to_play),
      style =
        TextStyle(
          color = GlanceTheme.colors.onSurface,
          fontFamily = FontFamily.SansSerif,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        ),
      maxLines = 1,
    )
    Spacer(GlanceModifier.height(if (size.height >= 176.dp) 8.dp else 4.dp))
    Text(
      text = context.getString(R.string.player_widget_open_shelfdroid),
      modifier = GlanceModifier.clickable(action),
      style =
        TextStyle(
          color = GlanceTheme.colors.primary,
          fontFamily = FontFamily.SansSerif,
          fontSize = 12.sp,
          fontWeight = FontWeight.Medium,
        ),
      maxLines = 1,
    )
  }
}
