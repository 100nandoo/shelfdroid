package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.playerWidgetOpenAction
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import dev.halim.shelfdroid.widget.player.widgetLaunchAction

@Composable
internal fun PlayerWidgetContent(content: PlayerWidgetData?) {
  val context = LocalContext.current
  val size = LocalSize.current
  val fontScale = context.resources.configuration.fontScale
  val layout =
    resolvePlayerWidgetLayout(
      width = size.width,
      height = size.height,
      fontScale = fontScale,
      hasChapterTitle = content?.chapterTitle?.isNotBlank() == true,
      showChapterNavigation = content?.showChapterNavigation == true,
    )
  val contentAlignment =
    if (content != null && layout !is PlayerWidgetLayoutSpec.Compact) {
      Alignment.TopCenter
    } else {
      Alignment.Center
    }
  val openAction =
    if (content == null || content.isPreview) {
      widgetLaunchAction(context)
    } else {
      playerWidgetOpenAction(context, content.id)
    }

  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .clickable(openAction)
        .background(GlanceTheme.colors.surface)
        .cornerRadius(R.dimen.player_widget_background_radius)
        .padding(horizontal = 8.dp, vertical = layout.verticalPadding),
    contentAlignment = contentAlignment,
  ) {
    if (content == null) {
      PlayerWidgetEmptyState()
    } else {
      when (layout) {
        is PlayerWidgetLayoutSpec.Compact ->
          CompactPlayerWidget(context, content, layout, openAction)

        is PlayerWidgetLayoutSpec.Medium -> MediumPlayerWidget(context, content, layout, openAction)

        is PlayerWidgetLayoutSpec.Large -> LargePlayerWidget(context, content, layout, openAction)
      }
    }
  }
}

@Composable
private fun CompactPlayerWidget(
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

@Composable
private fun MediumPlayerWidget(
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
        cover = content.cover,
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

@Composable
private fun LargePlayerWidget(
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
    Spacer(GlanceModifier.height(layout.contentSpacing))
    MainPlaybackControls(
      context = context,
      content = content,
      modifier = GlanceModifier.defaultWeight(),
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

@Composable
private fun WidgetTitle(
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
private fun WidgetAuthor(
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
private fun WidgetChapterTitle(
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
