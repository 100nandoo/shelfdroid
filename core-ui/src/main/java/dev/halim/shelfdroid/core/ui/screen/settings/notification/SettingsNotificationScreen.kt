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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.playback.PLAYBACK_SPEED_PRESET_VALUES
import dev.halim.shelfdroid.core.playback.SLEEP_TIMER_PRESET_MINUTES
import dev.halim.shelfdroid.core.playback.normalizePlaybackSpeedCycle
import dev.halim.shelfdroid.core.playback.normalizeSleepTimerCycle
import dev.halim.shelfdroid.core.prefs.MediaNotificationAction
import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.prefs.PlaybackSpeedNotificationMode
import dev.halim.shelfdroid.core.prefs.SleepTimerNotificationMode
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityVertical
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.extensions.toSpeedText
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.search.SettingsSearchSettingId
import dev.halim.shelfdroid.core.ui.screen.settings.search.SettingsSearchTarget
import dev.halim.shelfdroid.media.R as MediaR

@Composable
fun SettingsNotificationScreen(
  viewModel: SettingsNotificationViewModel = hiltViewModel(),
  searchTargetId: String? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsNotificationContent(uiState, searchTargetId, viewModel::onEvent)
}

@Composable
internal fun SettingsNotificationContent(
  uiState: SettingsNotificationUiState = SettingsNotificationUiState(),
  searchTargetId: String? = null,
  onEvent: (SettingsNotificationEvent) -> Unit = {},
) {
  val sleepTimerEnabled =
    uiState.firstAction == MediaNotificationAction.SleepTimer ||
      uiState.secondAction == MediaNotificationAction.SleepTimer
  val playbackSpeedEnabled =
    uiState.firstAction == MediaNotificationAction.PlaybackSpeed ||
      uiState.secondAction == MediaNotificationAction.PlaybackSpeed
  val searchResolution =
    resolveNotificationSearchTarget(
      searchTargetId,
      uiState,
      sleepTimerEnabled,
      playbackSpeedEnabled,
    )
  val resolvedSearchTargetId = searchResolution.targetSettingId
  val searchGuidance = searchResolution.guidanceResId?.let { stringResource(it) }
  val scrollConsumed = remember(searchTargetId) { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SettingsSearchTarget(
      settingId = SettingsSearchSettingId.NOTIFICATION_OPENING_SCREEN,
      label = stringResource(R.string.media_notification_opening_screen),
      targetSettingId = resolvedSearchTargetId,
      scrollConsumed = scrollConsumed,
    ) {
      Column { MediaNotificationOpeningScreenSection(uiState, onEvent) }
    }
    Spacer(Modifier.height(12.dp))
    SettingsSearchTarget(
      settingId = SettingsSearchSettingId.NOTIFICATION_PLAYER_PRESENTATION,
      label = stringResource(R.string.media_notification_player_presentation),
      targetSettingId = resolvedSearchTargetId,
      scrollConsumed = scrollConsumed,
    ) {
      Column { MediaNotificationPlayerPresentationSection(uiState, onEvent) }
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    MediaNotificationPreviewSection(uiState)
    SleepTimerSection(
      uiState,
      sleepTimerEnabled,
      onEvent,
      resolvedSearchTargetId,
      searchGuidance.takeIf { resolvedSearchTargetId == SettingsSearchSettingId.SLEEP_TIMER_MODE },
      scrollConsumed,
    )
    PlaybackSpeedSection(
      uiState,
      playbackSpeedEnabled,
      onEvent,
      resolvedSearchTargetId,
      searchGuidance.takeIf {
        resolvedSearchTargetId == SettingsSearchSettingId.PLAYBACK_SPEED_MODE
      },
      scrollConsumed,
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
    MediaNotificationSection(
      uiState,
      onEvent,
      resolvedSearchTargetId,
      searchGuidance,
      scrollConsumed,
    )
  }
}

internal data class NotificationSearchTargetResolution(
  val targetSettingId: String?,
  val guidanceResId: Int? = null,
)

private data class NotificationModeSearchGroup(
  val modeSettingId: String,
  val toggleDependentSettingId: String,
  val cycleDependentSettingId: String,
  val enabled: Boolean,
  val isToggleMode: Boolean,
  val unavailableGuidanceResId: Int,
  val chooseToggleGuidanceResId: Int,
  val chooseCycleGuidanceResId: Int,
)

internal fun resolveNotificationSearchTarget(
  settingId: String?,
  uiState: SettingsNotificationUiState,
  sleepTimerEnabled: Boolean,
  playbackSpeedEnabled: Boolean,
): NotificationSearchTargetResolution {
  val emptyActionSlot =
    when {
      uiState.firstAction == MediaNotificationAction.None ->
        SettingsSearchSettingId.NOTIFICATION_ACTION_ONE
      uiState.secondAction == MediaNotificationAction.None ->
        SettingsSearchSettingId.NOTIFICATION_ACTION_TWO
      else -> SettingsSearchSettingId.NOTIFICATION_ACTION_ONE
    }

  val sleepTimerGroup =
    NotificationModeSearchGroup(
      modeSettingId = SettingsSearchSettingId.SLEEP_TIMER_MODE,
      toggleDependentSettingId = SettingsSearchSettingId.DEFAULT_SLEEP_TIMER,
      cycleDependentSettingId = SettingsSearchSettingId.SLEEP_TIMER_CYCLE,
      enabled = sleepTimerEnabled,
      isToggleMode = uiState.sleepTimerMode == SleepTimerNotificationMode.Toggle,
      unavailableGuidanceResId = R.string.settings_search_assign_sleep_timer_action,
      chooseToggleGuidanceResId = R.string.settings_search_choose_sleep_timer_toggle,
      chooseCycleGuidanceResId = R.string.settings_search_choose_sleep_timer_cycle,
    )
  val playbackSpeedGroup =
    NotificationModeSearchGroup(
      modeSettingId = SettingsSearchSettingId.PLAYBACK_SPEED_MODE,
      toggleDependentSettingId = SettingsSearchSettingId.PLAYBACK_SPEED_TARGET,
      cycleDependentSettingId = SettingsSearchSettingId.PLAYBACK_SPEED_CYCLE,
      enabled = playbackSpeedEnabled,
      isToggleMode = uiState.playbackSpeedMode == PlaybackSpeedNotificationMode.Toggle,
      unavailableGuidanceResId = R.string.settings_search_assign_playback_speed_action,
      chooseToggleGuidanceResId = R.string.settings_search_choose_playback_speed_toggle,
      chooseCycleGuidanceResId = R.string.settings_search_choose_playback_speed_cycle,
    )

  return resolveNotificationModeSearchTarget(settingId, sleepTimerGroup, emptyActionSlot)
    ?: resolveNotificationModeSearchTarget(settingId, playbackSpeedGroup, emptyActionSlot)
    ?: NotificationSearchTargetResolution(settingId)
}

private fun resolveNotificationModeSearchTarget(
  settingId: String?,
  group: NotificationModeSearchGroup,
  emptyActionSlot: String,
): NotificationSearchTargetResolution? {
  if (
    settingId != group.modeSettingId &&
      settingId != group.toggleDependentSettingId &&
      settingId != group.cycleDependentSettingId
  ) {
    return null
  }

  val guidanceResId =
    when {
      !group.enabled -> group.unavailableGuidanceResId
      settingId == group.toggleDependentSettingId && !group.isToggleMode ->
        group.chooseToggleGuidanceResId
      settingId == group.cycleDependentSettingId && group.isToggleMode ->
        group.chooseCycleGuidanceResId
      else -> null
    }
  val targetSettingId =
    when {
      !group.enabled -> emptyActionSlot
      guidanceResId != null -> group.modeSettingId
      else -> settingId
    }

  return NotificationSearchTargetResolution(targetSettingId, guidanceResId)
}

@Composable
private fun SearchGuidance(text: String?) {
  if (text != null) {
    Text(
      text = text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
    )
  }
}

@Composable
private fun SleepTimerSection(
  uiState: SettingsNotificationUiState,
  enabled: Boolean,
  onEvent: (SettingsNotificationEvent) -> Unit,
  searchTargetId: String?,
  searchGuidance: String?,
  scrollConsumed: MutableState<Boolean>,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
    text = stringResource(R.string.sleep_timer),
  )
  val modeLabels =
    SleepTimerNotificationMode.entries.associateWith { stringResource(it.labelResId()) }
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.SLEEP_TIMER_MODE,
    label = stringResource(R.string.sleep_timer_notification_mode),
    targetSettingId = searchTargetId,
    scrollConsumed = scrollConsumed,
  ) {
    Column {
      ChipDropdownMenu(
        modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
        label = stringResource(R.string.sleep_timer_notification_mode),
        labelPosition = LabelPosition.Expand,
        options = SleepTimerNotificationMode.entries.map { it.name },
        initialValue = uiState.sleepTimerMode.name,
        optionLabel = { modeLabels.getValue(SleepTimerNotificationMode.valueOf(it)) },
        enabled = enabled,
        onClick = {
          onEvent(
            SettingsNotificationEvent.ChangeSleepTimerMode(SleepTimerNotificationMode.valueOf(it))
          )
        },
      )
      SearchGuidance(searchGuidance)
    }
  }
  Column {
    VisibilityVertical(visible = uiState.sleepTimerMode == SleepTimerNotificationMode.Toggle) {
      SettingsSearchTarget(
        settingId = SettingsSearchSettingId.DEFAULT_SLEEP_TIMER,
        label = stringResource(R.string.default_sleep_timer),
        targetSettingId = searchTargetId,
        scrollConsumed = scrollConsumed,
      ) {
        ChipDropdownMenu(
          modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
          label = stringResource(R.string.default_sleep_timer),
          labelPosition = LabelPosition.Expand,
          options = SLEEP_TIMER_PRESET_MINUTES.map { it.toString() },
          initialValue = uiState.sleepTimerMinutes.toString(),
          enabled = enabled,
          onClick = { selected ->
            selected.toIntOrNull()?.let {
              onEvent(SettingsNotificationEvent.ChangeSleepTimerMinutes(it))
            }
          },
        )
      }
    }
    VisibilityVertical(visible = uiState.sleepTimerMode == SleepTimerNotificationMode.Cyclical) {
      SettingsSearchTarget(
        settingId = SettingsSearchSettingId.SLEEP_TIMER_CYCLE,
        label = stringResource(R.string.sleep_timer_cycle),
        targetSettingId = searchTargetId,
        scrollConsumed = scrollConsumed,
      ) {
        Column { SleepTimerCycle(uiState.sleepTimerCycle, enabled, onEvent) }
      }
    }
  }
  Spacer(modifier = Modifier.height(24.dp))
}

private fun SleepTimerNotificationMode.labelResId(): Int =
  when (this) {
    SleepTimerNotificationMode.Toggle -> R.string.sleep_timer_mode_toggle
    SleepTimerNotificationMode.Cyclical -> R.string.sleep_timer_mode_cyclical
  }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SleepTimerCycle(
  selectedMinutes: List<Int>,
  enabled: Boolean,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  val cycle = normalizeSleepTimerCycle(selectedMinutes)
  TextTitleMedium(
    text = stringResource(R.string.sleep_timer_cycle),
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
  )
  Text(
    text = stringResource(R.string.sleep_timer_cycle_supporting_text),
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
  )
  Spacer(modifier = Modifier.height(8.dp))
  FlowRow(
    modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    SLEEP_TIMER_PRESET_MINUTES.forEach { minutes ->
      val selected = minutes in cycle
      FilterChip(
        selected = selected,
        enabled = enabled && (!selected || cycle.size > 1),
        onClick = {
          val updated = if (selected) cycle.filterNot { it == minutes } else cycle + minutes
          onEvent(SettingsNotificationEvent.ChangeSleepTimerCycle(updated))
        },
        label = { Text(stringResource(R.string.sleep_timer_cycle_minutes, minutes)) },
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

@Composable
private fun MediaNotificationOpeningScreenSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.media_notification_opening_screen),
  )
  Column(modifier = Modifier.selectableGroup()) {
    MediaNotificationOption(
      label = stringResource(R.string.media_notification_home),
      selected = uiState.openingScreen == MediaNotificationOpeningScreen.Home,
      onClick = {
        onEvent(SettingsNotificationEvent.ChangeOpeningScreen(MediaNotificationOpeningScreen.Home))
      },
    )
    MediaNotificationOption(
      label = stringResource(R.string.media_details),
      selected = uiState.openingScreen == MediaNotificationOpeningScreen.MediaDetails,
      onClick = {
        onEvent(
          SettingsNotificationEvent.ChangeOpeningScreen(MediaNotificationOpeningScreen.MediaDetails)
        )
      },
    )
  }
}

@Composable
private fun MediaNotificationPlayerPresentationSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.media_notification_player_presentation),
  )
  Column(modifier = Modifier.selectableGroup()) {
    MediaNotificationOption(
      label = stringResource(R.string.expanded_player),
      selected = uiState.playerPresentation == MediaNotificationPlayerPresentation.ExpandedPlayer,
      onClick = {
        onEvent(
          SettingsNotificationEvent.ChangePlayerPresentation(
            MediaNotificationPlayerPresentation.ExpandedPlayer
          )
        )
      },
    )
    MediaNotificationOption(
      label = stringResource(R.string.mini_player),
      selected = uiState.playerPresentation == MediaNotificationPlayerPresentation.MiniPlayer,
      onClick = {
        onEvent(
          SettingsNotificationEvent.ChangePlayerPresentation(
            MediaNotificationPlayerPresentation.MiniPlayer
          )
        )
      },
    )
  }
}

@Composable
private fun MediaNotificationPreviewSection(uiState: SettingsNotificationUiState) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.preview),
  )
  MediaNotificationPreview(uiState)
  Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun MediaNotificationSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
  searchTargetId: String?,
  searchGuidance: String?,
  scrollConsumed: MutableState<Boolean>,
) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.media_notification_buttons),
  )
  Spacer(modifier = Modifier.height(16.dp))
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.NOTIFICATION_ACTION_ONE,
    label = stringResource(R.string.media_notification_button_1),
    targetSettingId = searchTargetId,
    scrollConsumed = scrollConsumed,
  ) {
    Column {
      NotificationActionSlot(
        label = stringResource(R.string.media_notification_button_1),
        selected = uiState.firstAction,
        other = uiState.secondAction,
        onSelected = { onEvent(SettingsNotificationEvent.ChangeActionSlot(1, it)) },
      )
      SearchGuidance(
        searchGuidance.takeIf {
          searchTargetId == SettingsSearchSettingId.NOTIFICATION_ACTION_ONE
        }
      )
    }
  }
  Spacer(modifier = Modifier.height(8.dp))
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.NOTIFICATION_ACTION_TWO,
    label = stringResource(R.string.media_notification_button_2),
    targetSettingId = searchTargetId,
    scrollConsumed = scrollConsumed,
  ) {
    Column {
      NotificationActionSlot(
        label = stringResource(R.string.media_notification_button_2),
        selected = uiState.secondAction,
        other = uiState.firstAction,
        onSelected = { onEvent(SettingsNotificationEvent.ChangeActionSlot(2, it)) },
      )
      SearchGuidance(
        searchGuidance.takeIf {
          searchTargetId == SettingsSearchSettingId.NOTIFICATION_ACTION_TWO
        }
      )
    }
  }
  Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun MediaNotificationOption(label: String, selected: Boolean, onClick: () -> Unit) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
        .padding(horizontal = 16.dp, vertical = 4.dp)
        .padding(start = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = null)
    TextBodyMedium(
      text = label,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 8.dp),
    )
  }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun MediaNotificationPreview(uiState: SettingsNotificationUiState) {
  val colorScheme = MaterialTheme.colorScheme
  val contentColor = colorScheme.onSurface
  val configuredActions =
    listOf(uiState.firstAction, uiState.secondAction).filter {
      it != MediaNotificationAction.None
    }

  Box(
    modifier =
      Modifier.padding(horizontal = 16.dp)
        .fillMaxWidth()
        .clip(RoundedCornerShape(28.dp))
        .background(MaterialTheme.colorScheme.surfaceContainer)
        .padding(16.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(
          painter = painterResource(MediaR.drawable.ic_notification),
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(24.dp),
        )
      }
      Spacer(modifier = Modifier.size(24.dp))
      Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(
            text = stringResource(R.string.media_notification_preview_chapter),
            style =
              MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
              ),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          TextBodyMedium(
            text = stringResource(R.string.media_notification_preview_author),
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Box(
          modifier = Modifier.size(48.dp).clip(CircleShape).background(contentColor),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = painterResource(CoreR.drawable.play_arrow),
            contentDescription = stringResource(CoreR.string.play),
            tint = colorScheme.surface,
            modifier = Modifier.size(28.dp),
          )
        }
      }
      Spacer(modifier = Modifier.height(24.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MediaNotificationPreviewIcon(
          painter = painterResource(CoreR.drawable.fast_rewind),
          contentDescription = stringResource(CoreR.string.seek_back),
          tint = contentColor,
        )
        Spacer(modifier = Modifier.width(16.dp))
        Slider(
          state = rememberSliderState(value = 0.5f),
          onValueChange = {},
          modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
          thumb = {
            Box(
              modifier =
                Modifier.size(width = 6.dp, height = 18.dp)
                  .clip(RoundedCornerShape(4.dp))
                  .background(contentColor)
            )
          },
          track = { sliderState ->
            SliderDefaults.Track(
              modifier = Modifier.height(2.dp),
              sliderState = sliderState,
              drawStopIndicator = {},
              thumbTrackGapSize = 0.dp,
              colors =
                SliderDefaults.colors(
                  thumbColor = contentColor,
                  activeTrackColor = contentColor,
                  inactiveTrackColor = contentColor.copy(alpha = 0.24f),
                ),
            )
          },
        )
        Spacer(modifier = Modifier.width(16.dp))
        MediaNotificationPreviewIcon(
          painter = painterResource(CoreR.drawable.fast_forward),
          contentDescription = stringResource(CoreR.string.seek_forward),
          tint = contentColor,
        )
        configuredActions.forEach { action ->
          Spacer(modifier = Modifier.width(16.dp))
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
    modifier = Modifier.size(28.dp),
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
  val labels = MediaNotificationAction.entries.associateWith { stringResource(it.labelResId()) }
  val options =
    MediaNotificationAction.entries.filter {
      it == MediaNotificationAction.None || it == selected || it != other
    }
  ChipDropdownMenu(
    modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
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
private fun PlaybackSpeedSection(
  uiState: SettingsNotificationUiState,
  enabled: Boolean,
  onEvent: (SettingsNotificationEvent) -> Unit,
  searchTargetId: String?,
  searchGuidance: String?,
  scrollConsumed: MutableState<Boolean>,
) {
  val modeLabels =
    PlaybackSpeedNotificationMode.entries.associateWith {
      when (it) {
        PlaybackSpeedNotificationMode.Toggle -> stringResource(R.string.playback_speed_mode_toggle)
        PlaybackSpeedNotificationMode.Cyclical ->
          stringResource(R.string.playback_speed_mode_cyclical)
      }
    }
  TextTitleMedium(
    text = stringResource(R.string.playback_speed),
    modifier = Modifier.padding(horizontal = 16.dp).alpha(enabled.enableAlpha()),
  )
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.PLAYBACK_SPEED_MODE,
    label = stringResource(R.string.playback_speed_notification_mode),
    targetSettingId = searchTargetId,
    scrollConsumed = scrollConsumed,
  ) {
    Column {
      ChipDropdownMenu(
        modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
        label = stringResource(R.string.playback_speed_notification_mode),
        labelPosition = LabelPosition.Expand,
        options = PlaybackSpeedNotificationMode.entries.map { it.name },
        initialValue = uiState.playbackSpeedMode.name,
        optionLabel = { modeLabels.getValue(PlaybackSpeedNotificationMode.valueOf(it)) },
        enabled = enabled,
        onClick = {
          onEvent(
            SettingsNotificationEvent.ChangePlaybackSpeedMode(
              PlaybackSpeedNotificationMode.valueOf(it)
            )
          )
        },
      )
      SearchGuidance(searchGuidance)
    }
  }
  VisibilityVertical(visible = uiState.playbackSpeedMode == PlaybackSpeedNotificationMode.Toggle) {
    SettingsSearchTarget(
      settingId = SettingsSearchSettingId.PLAYBACK_SPEED_TARGET,
      label = stringResource(R.string.playback_speed_toggle_target),
      targetSettingId = searchTargetId,
      scrollConsumed = scrollConsumed,
    ) {
      ChipDropdownMenu(
        modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
        label = stringResource(R.string.playback_speed_toggle_target),
        labelPosition = LabelPosition.Expand,
        options = PLAYBACK_SPEED_PRESET_VALUES.filter { it != 1f }.map { it.toString() },
        initialValue = uiState.playbackSpeedToggleTarget.toString(),
        enabled = enabled,
        onClick = { selected ->
          selected.toFloatOrNull()?.let {
            onEvent(SettingsNotificationEvent.ChangePlaybackSpeedToggleTarget(it))
          }
        },
      )
    }
  }
  VisibilityVertical(
    visible = uiState.playbackSpeedMode == PlaybackSpeedNotificationMode.Cyclical
  ) {
    SettingsSearchTarget(
      settingId = SettingsSearchSettingId.PLAYBACK_SPEED_CYCLE,
      label = stringResource(R.string.playback_speed_cycle),
      targetSettingId = searchTargetId,
      scrollConsumed = scrollConsumed,
    ) {
      Column { PlaybackSpeedCycle(uiState, enabled, onEvent) }
    }
  }
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
    modifier = Modifier.padding(horizontal = 16.dp).padding(start = 8.dp).fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
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

@ShelfDroidPreview
@Composable
private fun SettingsNotificationContentPlaybackSpeedTogglePreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsNotificationContent(
      uiState =
        SettingsNotificationUiState(
          firstAction = MediaNotificationAction.PlaybackSpeed,
          secondAction = MediaNotificationAction.None,
          playbackSpeedMode = PlaybackSpeedNotificationMode.Toggle,
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SettingsNotificationContentCyclicalPreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsNotificationContent(
      uiState = SettingsNotificationUiState(sleepTimerMode = SleepTimerNotificationMode.Cyclical)
    )
  }
}
