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

private val compactHeight = 104.dp
private val largeHeight = 176.dp
private val minimumWidgetWidth = 220.dp

@Composable
internal fun PlayerWidgetContent(content: PlayerWidgetData?) {
  val context = LocalContext.current
  val size = LocalSize.current
  val layout = playerWidgetLayout(size.width, size.height)
  val verticalPadding = if (layout == PlayerWidgetLayout.Large) 2.dp else 4.dp

  Box(
    modifier =
      GlanceModifier.fillMaxSize()
        .background(GlanceTheme.colors.surface)
        .cornerRadius(R.dimen.player_widget_background_radius)
        .padding(horizontal = 8.dp, vertical = verticalPadding),
    contentAlignment = Alignment.Center,
  ) {
    if (content == null) {
      PlayerWidgetEmptyState()
    } else {
      val openPlayerAction =
        content.takeIf { it.coverResource == null }?.let { playerWidgetOpenAction(context, it.id) }
      when (layout) {
        PlayerWidgetLayout.Compact ->
          CompactPlayerWidget(context, content, size.height, openPlayerAction)
        PlayerWidgetLayout.Medium ->
          MediumPlayerWidget(context, content, size.height, openPlayerAction)
        PlayerWidgetLayout.Large ->
          LargePlayerWidget(context, content, size.height, openPlayerAction)
      }
    }
  }
}

private enum class PlayerWidgetLayout {
  Compact,
  Medium,
  Large,
}

private fun playerWidgetLayout(width: Dp, height: Dp): PlayerWidgetLayout =
  when {
    width < minimumWidgetWidth || height < compactHeight -> PlayerWidgetLayout.Compact
    height >= largeHeight -> PlayerWidgetLayout.Large
    else -> PlayerWidgetLayout.Medium
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
  val coverSize = minOf(72.dp, (height - 64.dp).coerceAtLeast(48.dp))
  val titleLines = if (height >= 144.dp) 2 else 1
  val chapterLines = if (height >= 160.dp) 2 else 1

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
    MainPlaybackControls(context, content, GlanceModifier.fillMaxWidth())
  }
}

@Composable
private fun LargePlayerWidget(
  context: Context,
  content: PlayerWidgetData,
  height: Dp,
  openPlayerAction: Action?,
) {
  val coverSize = minOf(96.dp, (height - 132.dp).coerceAtLeast(48.dp))
  val textLines = if (height >= 288.dp) 2 else 1
  val spacing = if (height >= 240.dp) 8.dp else 2.dp

  Column(
    modifier = GlanceModifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PlayerWidgetCover(
      cover = content.cover,
      size = coverSize,
      previewCoverResource = content.coverResource,
      action = openPlayerAction,
    )
    Spacer(GlanceModifier.height(spacing))
    when {
      content.title.isNotBlank() && content.author.isNotBlank() ->
        Row(
          modifier = GlanceModifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          WidgetTitle(content.title, textLines, openPlayerAction, GlanceModifier.defaultWeight())
          WidgetAuthor(content.author, GlanceModifier.defaultWeight())
        }
      content.title.isNotBlank() ->
        WidgetTitle(content.title, textLines, openPlayerAction, GlanceModifier.fillMaxWidth())
      content.author.isNotBlank() ->
        WidgetAuthor(content.author, GlanceModifier.fillMaxWidth())
    }
    content.chapterTitle?.let {
      WidgetChapterTitle(it, textLines, modifier = GlanceModifier.fillMaxWidth())
    }
    Spacer(GlanceModifier.height(spacing))
    MainPlaybackControls(context, content)
    if (content.showChapterNavigation) {
      Spacer(GlanceModifier.height(2.dp))
      ChapterNavigationControls(content)
    }
  }
}

@Composable
private fun WidgetTitle(
  title: String,
  maxLines: Int,
  action: Action?,
  modifier: GlanceModifier = GlanceModifier,
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
      ),
    maxLines = maxLines,
  )
}

@Composable
private fun WidgetAuthor(author: String, modifier: GlanceModifier = GlanceModifier) {
  Text(
    text = author,
    modifier = modifier,
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
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PlayerWidgetControlButton(
      icon = CoreR.drawable.fast_rewind,
      contentDescription = context.getString(CoreR.string.seek_back),
      enabled = content.seekBackEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_SEEK_BACK),
    )
    Spacer(GlanceModifier.width(2.dp))
    PlayerWidgetControlButton(
      icon = if (content.showPlayIcon) CoreR.drawable.play_arrow else CoreR.drawable.pause,
      contentDescription =
        context.getString(
          if (content.showPlayIcon) CoreR.string.play else CoreR.string.pause
        ),
      enabled = content.playPauseEnabled,
      tint = GlanceTheme.colors.onPrimary,
      action = widgetAction(WIDGET_ACTION_PLAY_PAUSE),
      background = GlanceTheme.colors.primary,
    )
    Spacer(GlanceModifier.width(2.dp))
    PlayerWidgetControlButton(
      icon = CoreR.drawable.fast_forward,
      contentDescription = context.getString(CoreR.string.seek_forward),
      enabled = content.seekForwardEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_SEEK_FORWARD),
    )
  }
}

@Composable
private fun ChapterNavigationControls(content: PlayerWidgetData) {
  val context = LocalContext.current
  Row(
    modifier = GlanceModifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    PlayerWidgetControlButton(
      icon = CoreR.drawable.skip_previous,
      contentDescription = context.getString(R.string.player_widget_previous_chapter),
      enabled = content.previousChapterEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_PREVIOUS_CHAPTER),
      size = 40.dp,
    )
    Spacer(GlanceModifier.width(8.dp))
    PlayerWidgetControlButton(
      icon = CoreR.drawable.skip_next,
      contentDescription = context.getString(R.string.player_widget_next_chapter),
      enabled = content.nextChapterEnabled,
      tint = GlanceTheme.colors.onSurfaceVariant,
      action = widgetAction(WIDGET_ACTION_NEXT_CHAPTER),
      size = 40.dp,
    )
  }
}

private fun playerWidgetOpenAction(context: Context, mediaId: String): Action {
  val intent =
    Intent(Helper.ACTION_OPEN_PLAYER).apply {
      setPackage(context.packageName)
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
