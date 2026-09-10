package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import dev.halim.shelfdroid.core.ui.extensions.toSpeedText
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsNotificationScreen(viewModel: SettingsNotificationViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsNotificationContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
private fun SettingsNotificationContent(
  uiState: SettingsNotificationUiState = SettingsNotificationUiState(),
  onEvent: (SettingsNotificationEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SleepTimerSection(uiState, onEvent)
    Spacer(modifier = Modifier.height(16.dp))
    MediaNotificationSection(uiState, onEvent)
  }
}

@Composable
private fun SleepTimerSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(text = stringResource(R.string.sleep_timer))
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
    label = stringResource(R.string.default_sleep_timer),
    labelPosition = LabelPosition.Expand,
    options = SLEEP_TIMER_PRESET_MINUTES.map { it.toString() },
    initialValue = uiState.sleepTimerMinutes.toString(),
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
  TextTitleMedium(text = stringResource(R.string.media_notification))
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
  Spacer(modifier = Modifier.height(16.dp))
  PlaybackSpeedCycle(uiState, onEvent)
}

@Composable
private fun NotificationActionSlot(
  label: String,
  selected: MediaNotificationAction,
  other: MediaNotificationAction,
  onSelected: (MediaNotificationAction) -> Unit,
) {
  val labels =
    mapOf(
      MediaNotificationAction.SleepTimer to stringResource(R.string.timer),
      MediaNotificationAction.NextChapter to stringResource(CoreR.string.next_chapter),
      MediaNotificationAction.PlaybackSpeed to stringResource(CoreR.string.playback_speed),
      MediaNotificationAction.None to stringResource(R.string.none),
    )
  val options =
    MediaNotificationAction.entries.filter {
      it == MediaNotificationAction.None || it == selected || it != other
    }
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
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
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  val speedCycle = normalizePlaybackSpeedCycle(uiState.playbackSpeedCycle)
  Text(text = stringResource(R.string.playback_speed_cycle))
  Text(
    text = stringResource(R.string.playback_speed_cycle_supporting_text),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
  Spacer(modifier = Modifier.height(8.dp))
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PLAYBACK_SPEED_PRESET_VALUES.forEach { speed ->
      val selected = speed in speedCycle
      FilterChip(
        selected = selected,
        enabled = !selected || speedCycle.size > 2,
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
