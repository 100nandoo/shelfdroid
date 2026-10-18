package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
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
  val launchIntent =
    context.packageManager.getLaunchIntentForPackage(context.packageName)
  val modifier =
    GlanceModifier.fillMaxSize().let { base ->
      launchIntent?.let { base.clickable(actionStartActivity(it)) } ?: base
    }

  Column(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = context.getString(R.string.player_widget_browse_library),
      style =
        TextStyle(
          color = GlanceTheme.colors.primary,
          fontFamily = FontFamily.SansSerif,
          fontSize = 16.sp,
          fontWeight = FontWeight.Medium,
        ),
      maxLines = 1,
    )
    Spacer(GlanceModifier.height(2.dp))
    Text(
      text = context.getString(R.string.player_widget_browse_library_hint),
      style =
        TextStyle(
          color = GlanceTheme.colors.onSurfaceVariant,
          fontFamily = FontFamily.SansSerif,
          fontSize = 12.sp,
          fontWeight = FontWeight.Normal,
        ),
      maxLines = 1,
    )
  }
}
