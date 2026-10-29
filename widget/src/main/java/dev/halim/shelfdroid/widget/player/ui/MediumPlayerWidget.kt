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
import androidx.glance.layout.height
import androidx.glance.layout.width
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import dev.halim.shelfdroid.widget.player.ui.components.WidgetAuthor
import dev.halim.shelfdroid.widget.player.ui.components.WidgetChapterTitle
import dev.halim.shelfdroid.widget.player.ui.components.WidgetTitle

@Composable
internal fun MediumPlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  layout: PlayerWidgetLayoutSpec.Medium,
  openPlayerAction: Action?,
) {
  Column(
    modifier = GlanceModifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      PlayerWidgetCover(
        bitmap = content.coverBitmap,
        size = layout.coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.width(8.dp))
      Column(modifier = GlanceModifier.defaultWeight()) {
        content.title
          .takeIf { it.isNotBlank() }
          ?.let {
            WidgetTitle(it, layout.titleMaxLines, openPlayerAction)
          }
        content.author.takeIf { it.isNotBlank() }?.let { WidgetAuthor(it) }
        content.chapterTitle?.let { WidgetChapterTitle(it, layout.chapterTitleMaxLines) }
      }
    }
    Spacer(GlanceModifier.height(layout.contentSpacing))
    MainPlaybackControls(
      context = context,
      content = content,
      modifier = GlanceModifier.fillMaxWidth(),
      showChapterNavigation = layout.showChapterNavigation,
      buttonSize = layout.controlButtonSize,
    )
  }
}
