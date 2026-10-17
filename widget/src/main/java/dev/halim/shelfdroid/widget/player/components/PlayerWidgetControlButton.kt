package dev.halim.shelfdroid.widget.player.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider

@Composable
internal fun PlayerWidgetControlButton(
  icon: Int,
  contentDescription: String,
  enabled: Boolean,
  tint: Color,
  action: Action,
  background: Color? = null,
) {
  var modifier =
    GlanceModifier.size(48.dp)
      .background(ColorProvider(background ?: Color.Transparent))
      .cornerRadius(24.dp)
  if (enabled) modifier = modifier.clickable(action)

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Image(
      provider = ImageProvider(icon),
      contentDescription = contentDescription,
      modifier = GlanceModifier.size(24.dp),
      colorFilter = ColorFilter.tint(ColorProvider(tint.copy(alpha = if (enabled) 1f else 0.38f))),
    )
  }
}
