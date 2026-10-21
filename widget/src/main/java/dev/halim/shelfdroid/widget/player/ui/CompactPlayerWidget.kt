package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.width
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import dev.halim.shelfdroid.widget.player.ui.components.WidgetAuthor
import dev.halim.shelfdroid.widget.player.ui.components.WidgetTitle

@Composable
internal fun CompactPlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  layout: PlayerWidgetLayoutSpec.Compact,
  openPlayerAction: Action?,
) {
  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (layout.showCover) {
      PlayerWidgetCover(
        cover = content.cover,
        size = layout.coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.width(8.dp))
    }
    Column(modifier = GlanceModifier.defaultWeight()) {
      content.title
        .takeIf { it.isNotBlank() }
        ?.let {
          WidgetTitle(
            title = it,
            maxLines = layout.titleMaxLines,
            action = openPlayerAction,
          )
        }
      content.author.takeIf { it.isNotBlank() }?.let { WidgetAuthor(it) }
    }
    Spacer(GlanceModifier.width(8.dp))
    MainPlaybackControls(
      context = context,
      content = content,
      showChapterNavigation = false,
      buttonSize = layout.controlButtonSize,
    )
  }
}
