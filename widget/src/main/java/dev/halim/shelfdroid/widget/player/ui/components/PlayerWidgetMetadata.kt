package dev.halim.shelfdroid.widget.player.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle

@Composable
internal fun WidgetTitle(
  title: String,
  maxLines: Int,
  action: Action?,
  modifier: GlanceModifier = GlanceModifier,
  textAlign: TextAlign = TextAlign.Start,
) {
  Text(
    text = title,
    modifier = action?.let { modifier.clickable(it) } ?: modifier,
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurface,
        fontFamily = FontFamily.Serif,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        textAlign = textAlign,
      ),
    maxLines = maxLines,
  )
}

@Composable
internal fun WidgetAuthor(
  author: String,
  modifier: GlanceModifier = GlanceModifier,
  textAlign: TextAlign = TextAlign.Start,
) {
  Text(
    text = author,
    modifier = modifier,
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurfaceVariant,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        textAlign = textAlign,
      ),
    maxLines = 1,
  )
}

@Composable
internal fun WidgetChapterTitle(
  title: String,
  maxLines: Int,
  modifier: GlanceModifier = GlanceModifier,
) {
  Text(
    text = title,
    modifier = modifier,
    style =
      TextStyle(
        color = GlanceTheme.colors.onSurfaceVariant,
        fontFamily = FontFamily.SansSerif,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.Start,
      ),
    maxLines = maxLines,
  )
}
