package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.playback.PLAYBACK_SPEED_PRESET_VALUES
import dev.halim.shelfdroid.core.playback.SLEEP_TIMER_PRESET_MINUTES
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.extensions.toSpeedText
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.media.R as MediaR

@Composable
fun SettingsNotificationScreen(viewModel: SettingsNotificationViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsNotificationContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
internal fun SettingsNotificationContent(
  uiState: SettingsNotificationUiState = SettingsNotificationUiState(),
  onEvent: (SettingsNotificationEvent) -> Unit = {},
) {
  val sleepTimerEnabled =
    uiState.firstAction == MediaNotificationAction.SleepTimer ||
      uiState.secondAction == MediaNotificationAction.SleepTimer
  val playbackSpeedEnabled =
    uiState.firstAction == MediaNotificationAction.PlaybackSpeed ||
      uiState.secondAction == MediaNotificationAction.PlaybackSpeed

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SleepTimerSection(uiState, sleepTimerEnabled, onEvent)
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    PlaybackSpeedCycle(uiState, playbackSpeedEnabled, onEvent)
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    MediaNotificationSection(uiState, onEvent)
  }
}

@Composable
private fun SleepTimerSection(
  uiState: SettingsNotificationUiState,
  enabled: Boolean,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
    text = stringResource(R.string.sleep_timer),
  )
  ChipDropdownMenu(
    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    label = stringResource(R.string.default_sleep_timer),
    labelPosition = LabelPosition.Expand,
    options = SLEEP_TIMER_PRESET_MINUTES.map { it.toString() },
    initialValue = uiState.sleepTimerMinutes.toString(),
    enabled = enabled,
    onClick = { selected ->
      selected.toIntOrNull()?.let { onEvent(SettingsNotificationEvent.ChangeSleepTimerMinutes(it)) }
    },
  )
}

@Composable
private fun MediaNotificationSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.media_notification),
  )
  MediaNotificationPreview(uiState)
  Spacer(modifier = Modifier.height(16.dp))
  NotificationActionSlot(
    label = stringResource(R.string.media_notification_button_1),
    selected = uiState.firstAction,
    other = uiState.secondAction,
    onSelected = { onEvent(SettingsNotificationEvent.ChangeActionSlot(1, it)) },
  )
  Spacer(modifier = Modifier.height(8.dp))
  NotificationActionSlot(
    label = stringResource(R.string.media_notification_button_2),
    selected = uiState.secondAction,
    other = uiState.firstAction,
    onSelected = { onEvent(SettingsNotificationEvent.ChangeActionSlot(2, it)) },
  )
}

@Composable
private fun MediaNotificationPreview(uiState: SettingsNotificationUiState) {
  val colorScheme = MaterialTheme.colorScheme
  val contentColor = colorScheme.onSurface
  val configuredActions =
    listOf(uiState.firstAction, uiState.secondAction).filter {
      it != MediaNotificationAction.None
    }

  Box(
    modifier =
      Modifier
        .padding(horizontal = 16.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(
          Brush.linearGradient(
            listOf(
              colorScheme.primaryContainer,
              colorScheme.secondaryContainer,
              colorScheme.tertiaryContainer,
            )
          )
        )
        .padding(16.dp)
  ) {
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .background(colorScheme.surface.copy(alpha = 0.22f))
    )
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(MediaR.drawable.ic_notification),
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
          text = stringResource(R.string.media_notification_preview_app_name),
          style = MaterialTheme.typography.labelMedium,
          color = contentColor,
        )
      }
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = stringResource(R.string.media_notification_preview_title),
        style = MaterialTheme.typography.titleLarge,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(R.string.media_notification_preview_author),
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = stringResource(R.string.media_notification_preview_chapter),
        style = MaterialTheme.typography.bodySmall,
        color = contentColor.copy(alpha = 0.8f),
      )
      Spacer(modifier = Modifier.height(12.dp))
      LinearProgressIndicator(
        progress = { 0.6f },
        modifier = Modifier.fillMaxWidth(),
        color = contentColor,
        trackColor = contentColor.copy(alpha = 0.24f),
      )
      Spacer(modifier = Modifier.height(8.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MediaNotificationPreviewIcon(
          painter = painterResource(R.drawable.fast_rewind),
          contentDescription = stringResource(R.string.seek_back),
          tint = contentColor,
        )
        Box(
          modifier =
            Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(contentColor),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(R.drawable.play_arrow),
            contentDescription = stringResource(R.string.play),
            tint = colorScheme.surface,
            modifier = Modifier.size(28.dp),
          )
        }
        MediaNotificationPreviewIcon(
          painter = painterResource(R.drawable.fast_forward),
          contentDescription = stringResource(R.string.seek_forward),
          tint = contentColor,
        )
        configuredActions.forEach { action ->
          MediaNotificationPreviewIcon(
            painter = painterResource(action.previewIconResId()),
            contentDescription = action.previewContentDescription(),
            tint = contentColor,
          )
        }
      }
    }
  }
}

@Composable
private fun MediaNotificationPreviewIcon(
  painter: Painter,
  contentDescription: String,
  tint: Color,
) {
  Icon(
    painter = painter,
    contentDescription = contentDescription,
    tint = tint,
    modifier = Modifier.size(32.dp),
  )
}

private fun MediaNotificationAction.previewIconResId(): Int =
  when (this) {
    MediaNotificationAction.SleepTimer -> CoreR.drawable.timer
    MediaNotificationAction.NextChapter -> CoreR.drawable.skip_next
    MediaNotificationAction.PreviousChapter -> CoreR.drawable.skip_previous
    MediaNotificationAction.PlaybackSpeed -> CoreR.drawable.speed
    MediaNotificationAction.None -> error("None has no preview icon")
  }

@Composable
private fun MediaNotificationAction.previewContentDescription(): String =
  stringResource(labelResId())

private fun MediaNotificationAction.labelResId(): Int =
  when (this) {
    MediaNotificationAction.SleepTimer -> R.string.timer
    MediaNotificationAction.NextChapter -> CoreR.string.next_chapter
    MediaNotificationAction.PreviousChapter -> CoreR.string.previous_chapter
    MediaNotificationAction.PlaybackSpeed -> CoreR.string.playback_speed
    MediaNotificationAction.None -> R.string.none
  }

@Composable
private fun NotificationActionSlot(
  label: String,
  selected: MediaNotificationAction,
  other: MediaNotificationAction,
  onSelected: (MediaNotificationAction) -> Unit,
) {
  val labels =
    MediaNotificationAction.entries.associateWith { stringResource(it.labelResId()) }
  val options =
    MediaNotificationAction.entries.filter {
      it == MediaNotificationAction.None || it == selected || it != other
    }
  ChipDropdownMenu(
    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    label = label,
    labelPosition = LabelPosition.Expand,
    options = options.map { it.name },
    initialValue = selected.name,
    optionLabel = { labels.getValue(MediaNotificationAction.valueOf(it)) },
    onClick = { value -> onSelected(MediaNotificationAction.valueOf(value)) },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackSpeedCycle(
  uiState: SettingsNotificationUiState,
  enabled: Boolean,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  val speedCycle = normalizePlaybackSpeedCycle(uiState.playbackSpeedCycle)
  TextTitleMedium(
    text = stringResource(R.string.playback_speed_cycle),
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
  )
  Text(
    text = stringResource(R.string.playback_speed_cycle_supporting_text),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
  )
  Spacer(modifier = Modifier.height(8.dp))
  FlowRow(
    modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PLAYBACK_SPEED_PRESET_VALUES.forEach { speed ->
      val selected = speed in speedCycle
      FilterChip(
        selected = selected,
        enabled = enabled && (!selected || speedCycle.size > 2),
        onClick = {
          val updated =
            if (selected) {
              speedCycle.filterNot { it == speed }
            } else {
              speedCycle + speed
            }
          onEvent(SettingsNotificationEvent.ChangePlaybackSpeedCycle(updated))
        },
        label = { Text(text = "${speed.toSpeedText()}x") },
        leadingIcon =
          if (selected) {
            {
              Icon(
                painter = painterResource(R.drawable.check),
                contentDescription = stringResource(R.string.selected),
                modifier = Modifier.size(FilterChipDefaults.IconSize),
              )
            }
          } else null,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun SettingsNotificationContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsNotificationContent() }
}

@ShelfDroidPreview
@Composable
private fun SettingsNotificationContentDisabledPreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsNotificationContent(
      uiState =
        SettingsNotificationUiState(
          firstAction = MediaNotificationAction.None,
          secondAction = MediaNotificationAction.NextChapter,
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SettingsNotificationContentPlaybackSpeedPreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsNotificationContent(
      uiState =
        SettingsNotificationUiState(
          firstAction = MediaNotificationAction.PlaybackSpeed,
          secondAction = MediaNotificationAction.None,
        )
    )
  }
}
