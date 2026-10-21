package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.layout.Alignment
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.width
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.actions.WIDGET_ACTION_NEXT_CHAPTER
import dev.halim.shelfdroid.widget.player.actions.WIDGET_ACTION_PLAY_PAUSE
import dev.halim.shelfdroid.widget.player.actions.WIDGET_ACTION_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.widget.player.actions.WIDGET_ACTION_SEEK_BACK
import dev.halim.shelfdroid.widget.player.actions.WIDGET_ACTION_SEEK_FORWARD
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetControlButton
import dev.halim.shelfdroid.widget.player.actions.widgetAction

@Composable
internal fun MainPlaybackControls(
  context: Context,
  content: PlayerWidgetData,
  showChapterNavigation: Boolean,
  buttonSize: Dp,
  modifier: GlanceModifier = GlanceModifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (showChapterNavigation) {
      PlayerWidgetControlButton(
        icon = CoreR.drawable.skip_previous,
        contentDescription = context.getString(R.string.player_widget_previous_chapter),
        enabled = content.previousChapterEnabled,
        tint = GlanceTheme.colors.onSurfaceVariant,
        action = widgetAction(WIDGET_ACTION_PREVIOUS_CHAPTER),
        size = buttonSize,
      )
      Spacer(GlanceModifier.width(2.dp))
    }
    PlayerWidgetControlButton(
      icon = CoreR.drawable.fast_rewind,
      contentDescription = context.getString(CoreR.string.seek_back),
      enabled = content.seekBackEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_SEEK_BACK),
      size = buttonSize,
    )
    Spacer(GlanceModifier.width(2.dp))
    PlayerWidgetControlButton(
      icon = if (content.showPlayIcon) CoreR.drawable.play_arrow else CoreR.drawable.pause,
      contentDescription =
        context.getString(if (content.showPlayIcon) CoreR.string.play else CoreR.string.pause),
      enabled = content.playPauseEnabled,
      tint = GlanceTheme.colors.onPrimary,
      action = widgetAction(WIDGET_ACTION_PLAY_PAUSE),
      background = GlanceTheme.colors.primary,
      size = buttonSize,
    )
    Spacer(GlanceModifier.width(2.dp))
    PlayerWidgetControlButton(
      icon = CoreR.drawable.fast_forward,
      contentDescription = context.getString(CoreR.string.seek_forward),
      enabled = content.seekForwardEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_SEEK_FORWARD),
      size = buttonSize,
    )
    if (showChapterNavigation) {
      Spacer(GlanceModifier.width(2.dp))
      PlayerWidgetControlButton(
        icon = CoreR.drawable.skip_next,
        contentDescription = context.getString(R.string.player_widget_next_chapter),
        enabled = content.nextChapterEnabled,
        tint = GlanceTheme.colors.onSurfaceVariant,
        action = widgetAction(WIDGET_ACTION_NEXT_CHAPTER),
        size = buttonSize,
      )
    }
  }
}

internal fun playerWidgetControlButtonSize(width: Dp, showChapterNavigation: Boolean): Dp {
  val controlCount = if (showChapterNavigation) 5 else 3
  val availableWidth = width - playbackControlsHorizontalPadding
  val spacing = playbackControlButtonSpacing * (controlCount - 1).toFloat()
  return minOf(
    maximumPlaybackControlButtonSize,
    ((availableWidth - spacing) / controlCount.toFloat()).coerceAtLeast(
      minimumPlaybackControlButtonSize
    ),
  )
}

private val playbackControlsHorizontalPadding = 16.dp
private val playbackControlButtonSpacing = 2.dp
private val maximumPlaybackControlButtonSize = 48.dp
private val minimumPlaybackControlButtonSize = 1.dp
