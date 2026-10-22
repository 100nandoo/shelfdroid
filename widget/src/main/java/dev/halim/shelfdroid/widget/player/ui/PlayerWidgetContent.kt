package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import dev.halim.shelfdroid.widget.R
import dev.halim.shelfdroid.widget.player.PlayerWidgetData
import dev.halim.shelfdroid.widget.player.actions.playerWidgetOpenAction
import dev.halim.shelfdroid.widget.player.actions.widgetLaunchAction

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
