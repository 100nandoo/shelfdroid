package dev.halim.shelfdroid.widget.player.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
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
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.helper.Helper
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.WIDGET_ACTION_NEXT_CHAPTER
import dev.halim.shelfdroid.widget.player.WIDGET_ACTION_PLAY_PAUSE
import dev.halim.shelfdroid.widget.player.WIDGET_ACTION_PREVIOUS_CHAPTER
import dev.halim.shelfdroid.widget.player.WIDGET_ACTION_SEEK_BACK
import dev.halim.shelfdroid.widget.player.WIDGET_ACTION_SEEK_FORWARD
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetControlButton
import dev.halim.shelfdroid.widget.player.ui.components.PlayerWidgetCover
import dev.halim.shelfdroid.widget.player.widgetAction
import dev.halim.shelfdroid.widget.player.widgetLaunchAction
import dev.halim.shelfdroid.widget.player.widgetMainActivityComponent

private val compactHeight = 116.dp
private val largeHeight = 176.dp
private val largeAboveCoverHeight = 302.dp
private val minimumWidgetWidth = 220.dp

@Composable
internal fun PlayerWidgetContent(content: PlayerWidgetData?) {
  val context = LocalContext.current
  val size = LocalSize.current
  val fontScale = context.resources.configuration.fontScale.coerceAtLeast(1f)
  val layout = playerWidgetLayout(size.width, size.height, fontScale)
  val verticalPadding = if (layout == PlayerWidgetLayout.Compact) 4.dp else 8.dp
  val contentAlignment =
    if (content != null && layout != PlayerWidgetLayout.Compact) {
      Alignment.TopCenter
    } else {
      Alignment.Center
    }
  val openAction =
    content?.takeIf { it.coverResource == null }?.let { playerWidgetOpenAction(context, it.id) }
      ?: widgetLaunchAction(context)

  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .clickable(openAction)
        .background(GlanceTheme.colors.surface)
        .cornerRadius(R.dimen.player_widget_background_radius)
        .padding(horizontal = 8.dp, vertical = verticalPadding),
    contentAlignment = contentAlignment,
  ) {
    if (content == null) {
      PlayerWidgetEmptyState()
    } else {
      when (layout) {
        PlayerWidgetLayout.Compact ->
          CompactPlayerWidget(context, content, size.height, openAction)

        PlayerWidgetLayout.Medium ->
          MediumPlayerWidget(context, content, size.height, openAction)

        PlayerWidgetLayout.Large ->
          LargePlayerWidget(context, content, size.height, openAction)
      }
    }
  }
}

private enum class PlayerWidgetLayout {
  Compact,
  Medium,
  Large,
}

private fun playerWidgetLayout(width: Dp, height: Dp, fontScale: Float): PlayerWidgetLayout {
  val mediumHeight = compactHeight + 48.dp * (fontScale - 1f)
  return when {
    width < minimumWidgetWidth || height < mediumHeight -> PlayerWidgetLayout.Compact
    height >= largeHeight -> PlayerWidgetLayout.Large
    else -> PlayerWidgetLayout.Medium
  }
}

@Composable
private fun CompactPlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  height: Dp,
  openPlayerAction: Action?,
) {
  val coverSize = minOf(48.dp, (height - 8.dp).coerceAtLeast(1.dp))

  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (LocalSize.current.width >= 280.dp) {
      PlayerWidgetCover(
        cover = content.cover,
        size = coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.width(8.dp))
    }
    Column(modifier = GlanceModifier.defaultWeight()) {
      content.title.takeIf { it.isNotBlank() }?.let {
        WidgetTitle(
          title = it,
          maxLines = if (height < 80.dp) 1 else 2,
          action = openPlayerAction,
        )
      }
      content.author.takeIf { it.isNotBlank() }?.let { WidgetAuthor(it) }
    }
    Spacer(GlanceModifier.width(8.dp))
    MainPlaybackControls(context, content)
  }
}

@Composable
private fun MediumPlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  height: Dp,
  openPlayerAction: Action?,
) {
  val coverSize = minOf(72.dp, (height - 68.dp).coerceAtLeast(48.dp))
  val fontScale = LocalContext.current.resources.configuration.fontScale.coerceAtLeast(1f)
  val metadataHeight = height - 56.dp
  val titleLines = if (metadataHeight >= 66.dp * fontScale) 2 else 1
  val chapterLines = if (metadataHeight >= 80.dp * fontScale) 2 else 1

  Column(
    modifier = GlanceModifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      PlayerWidgetCover(
        cover = content.cover,
        size = coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.width(8.dp))
      Column(modifier = GlanceModifier.defaultWeight()) {
        content.title.takeIf { it.isNotBlank() }?.let {
          WidgetTitle(it, titleLines, openPlayerAction)
        }
        content.author.takeIf { it.isNotBlank() }?.let { WidgetAuthor(it) }
        content.chapterTitle?.let { WidgetChapterTitle(it, chapterLines) }
      }
    }
    Spacer(GlanceModifier.height(4.dp))
    MainPlaybackControls(
      context = context,
      content = content,
      modifier = GlanceModifier.fillMaxWidth(),
      showChapterNavigation = content.showChapterNavigation,
      buttonSize = playerWidgetControlButtonSize(content.showChapterNavigation),
    )
  }
}

@Composable
private fun LargePlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  height: Dp,
  openPlayerAction: Action?,
) {
  val fontScale = LocalContext.current.resources.configuration.fontScale.coerceAtLeast(1f)
  val spacing = if (height >= 240.dp) 8.dp else 2.dp
  // Keep art and two-line metadata from shrinking at the side-to-stacked transition.
  val sideCover = height < largeAboveCoverHeight || fontScale > 1f
  val coverSize =
    if (sideCover) {
      minOf(96.dp, (height - 108.dp).coerceAtLeast(48.dp))
    } else {
      minOf(96.dp, (height - 164.dp).coerceAtLeast(48.dp))
    }
  val metadataHeight =
    if (sideCover) {
      height - 100.dp - (spacing * 3)
    } else {
      height - coverSize - 100.dp - (spacing * 3)
    }
  val chapterHeadline = content.chapterTitle?.takeIf { it.isNotBlank() }
  val headline = chapterHeadline ?: content.title.takeIf { it.isNotBlank() }
  val headlineTwoLineHeight = if (chapterHeadline != null) 82.dp else 68.dp
  val headlineLines = if (metadataHeight >= headlineTwoLineHeight * fontScale) 2 else 1

  Column(
    modifier = GlanceModifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    if (sideCover) {
      Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        PlayerWidgetCover(
          cover = content.cover,
          size = coverSize,
          previewCoverResource = content.coverResource,
          action = openPlayerAction,
        )
        Spacer(GlanceModifier.width(8.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
          LargePlayerMetadata(headline, headlineLines, content.author, openPlayerAction)
        }
      }
    } else {
      PlayerWidgetCover(
        cover = content.cover,
        size = coverSize,
        previewCoverResource = content.coverResource,
        action = openPlayerAction,
      )
      Spacer(GlanceModifier.height(spacing))
      LargePlayerMetadata(headline, headlineLines, content.author, openPlayerAction)
    }
    Spacer(GlanceModifier.height(spacing))
    MainPlaybackControls(
      context = context,
      content = content,
      showChapterNavigation = content.showChapterNavigation,
      buttonSize = playerWidgetControlButtonSize(content.showChapterNavigation),
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
  author.takeIf { it.isNotBlank() }?.let {
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

@Composable
private fun MainPlaybackControls(
  context: Context,
  content: PlayerWidgetData,
  modifier: GlanceModifier = GlanceModifier,
  showChapterNavigation: Boolean = false,
  buttonSize: Dp = 48.dp,
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
        context.getString(
          if (content.showPlayIcon) CoreR.string.play else CoreR.string.pause,
        ),
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

@Composable
private fun playerWidgetControlButtonSize(showChapterNavigation: Boolean): Dp {
  val controlCount = if (showChapterNavigation) 5 else 3
  val availableWidth = LocalSize.current.width - 16.dp
  val spacing = 2.dp * (controlCount - 1).toFloat()
  return minOf(
    48.dp,
    ((availableWidth - spacing) / controlCount.toFloat()).coerceAtLeast(1.dp),
  )
}

private fun playerWidgetOpenAction(context: Context, mediaId: String): Action {
  val intent =
    Intent(Helper.ACTION_OPEN_PLAYER).apply {
      component = widgetMainActivityComponent(context)
      putExtra(Helper.EXTRA_MEDIA_ID, mediaId)
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_OPENING_SCREEN,
        MediaNotificationOpeningScreen.Home.name,
      )
      putExtra(
        Helper.EXTRA_MEDIA_NOTIFICATION_PLAYER_PRESENTATION,
        MediaNotificationPlayerPresentation.ExpandedPlayer.name,
      )
      flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
  return actionStartActivity(intent)
}
