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
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.glance.text.TextAlign
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import dev.halim.shelfdroid.widget.player.ui.components.WidgetAuthor
import dev.halim.shelfdroid.widget.player.ui.components.WidgetTitle

@Composable
internal fun LargePlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  layout: PlayerWidgetLayoutSpec.Large,
  openPlayerAction: Action?,
) {
  val chapterHeadline = content.chapterTitle?.takeIf { it.isNotBlank() }
  val headline = chapterHeadline ?: content.title.takeIf { it.isNotBlank() }

  Column(
    modifier = GlanceModifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (layout.sideCover) {
      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayerWidgetCover(
          cover = content.cover,
          size = layout.coverSize,
          previewCoverResource = content.coverResource,
          action = openPlayerAction,
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
          LargePlayerMetadata(
            headline,
            layout.headlineMaxLines,
            content.author,
            openPlayerAction,
          )
        }
      }
    } else {
      PlayerWidgetCover(
        cover = content.cover,
        size = layout.coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.height(layout.contentSpacing))
      LargePlayerMetadata(headline, layout.headlineMaxLines, content.author, openPlayerAction)
    }
    Spacer(GlanceModifier.defaultWeight())
    Spacer(GlanceModifier.height(layout.contentSpacing))
    MainPlaybackControls(
      context = context,
      content = content,
      modifier = GlanceModifier.fillMaxWidth().height(SharedMetrics.controlButtonSize),
      showChapterNavigation = layout.showChapterNavigation,
      buttonSize = layout.controlButtonSize,
    )
  }
}

@Composable
private fun LargePlayerMetadata(
  headline: String?,
  headlineLines: Int,
  author: String,
  openPlayerAction: Action?,
) {
  headline?.let {
    WidgetTitle(
      title = it,
      maxLines = headlineLines,
      action = openPlayerAction,
      modifier = GlanceModifier.fillMaxWidth(),
      textAlign = TextAlign.Center,
    )
  }
  author
    .takeIf { it.isNotBlank() }
    ?.let {
      WidgetAuthor(it, GlanceModifier.fillMaxWidth(), TextAlign.Center)
    }
}
