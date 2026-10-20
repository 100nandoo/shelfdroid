package dev.halim.shelfdroid.widget.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.size
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import dev.halim.shelfdroid.widget.player.DisabledWidgetControlAction

@Composable
internal fun PlayerWidgetControlButton(
  icon: Int,
  contentDescription: String,
  enabled: Boolean,
  tint: ColorProvider,
  action: Action,
  background: ColorProvider? = null,
  size: Dp = 48.dp,
) {
  val modifier =
    GlanceModifier.fillMaxSize()
      .background(background ?: ColorProvider(Color.Transparent))
      .cornerRadius(size / 2)
      .let { if (enabled) it.clickable(action) else it }

  Box(
    modifier =
      GlanceModifier.size(size).clickable(actionRunCallback<DisabledWidgetControlAction>()),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = modifier.semantics { this.contentDescription = contentDescription },
      contentAlignment = Alignment.Center,
    ) {
      Image(
        provider = ImageProvider(icon),
        contentDescription = null,
        modifier = GlanceModifier.size(24.dp),
        colorFilter = ColorFilter.tint(if (enabled) tint else GlanceTheme.colors.outline),
      )
    }
  }
}
