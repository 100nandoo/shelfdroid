@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleMode
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastScheduleSimpleInterval
import dev.halim.shelfdroid.core.data.screen.edititem.canConfigureSchedule
import dev.halim.shelfdroid.core.data.screen.edititem.hasScheduleChanges
import dev.halim.shelfdroid.core.data.screen.edititem.simpleScheduleGuidance
import dev.halim.shelfdroid.core.data.screen.edititem.validationMessage
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CheckboxRow
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.SectionCard
import dev.halim.shelfdroid.core.ui.components.TimePickerTextField
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun ScheduleTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit = {}) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.Bottom),
    modifier = Modifier.fillMaxSize(),
  ) {
    if (!uiState.canConfigureSchedule()) {
      item(key = "missing-rss") {
        Column(modifier = Modifier.animateItem()) { MissingRssWarningCard() }
      }
      return@LazyColumn
    }

    if (uiState.schedule.autoDownloadEpisodes) {
      item(key = "max-episodes-to-keep") {
        MyOutlinedTextField(
          modifier = Modifier.animateItem(),
          enabled = !uiState.isSaving,
          value = uiState.schedule.maxEpisodesToKeepInput,
          onValueChange = {
            onEvent(EditItemEvent.UpdateScheduleMaxEpisodesToKeepInput(it))
          },
          label = stringResource(R.string.edit_item_schedule_max_episodes_to_keep),
          placeholder = stringResource(R.string.edit_item_schedule_limit_hint),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
      }

      item(key = "max-new-episodes") {
        MyOutlinedTextField(
          modifier = Modifier.animateItem(),
          enabled = !uiState.isSaving,
          value = uiState.schedule.maxNewEpisodesToDownloadInput,
          onValueChange = {
            onEvent(EditItemEvent.UpdateScheduleMaxNewEpisodesToDownloadInput(it))
          },
          label = stringResource(R.string.edit_item_schedule_max_new_episodes),
          placeholder = stringResource(R.string.edit_item_schedule_limit_hint),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        )
      }

      item(key = "schedule-mode") {
        Spacer(Modifier.height(24.dp))
        ScheduleModeSelector(
          modifier = Modifier.animateItem(),
          uiState = uiState,
          onEvent = onEvent,
        )
      }

      when (uiState.scheduleMode) {
        PodcastScheduleMode.Simple -> simpleScheduleItems(uiState = uiState, onEvent = onEvent)
        PodcastScheduleMode.Advanced -> {
          item(key = "advanced-schedule") {
            AdvancedScheduleEditor(
              modifier = Modifier.animateItem(),
              uiState = uiState,
              onEvent = onEvent,
            )
          }
        }
      }
    } else {
      item(key = "schedule-disabled-hint") {
        Text(
          modifier = Modifier.animateItem(),
          text = stringResource(R.string.edit_item_schedule_disabled_hint),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    item(key = "save-schedule") {
      Column(
        modifier = Modifier.fillMaxWidth().animateItem(),
        horizontalAlignment = Alignment.End,
      ) {
        Spacer(Modifier.height(8.dp))
        Button(
          enabled = !uiState.isSaving && uiState.hasScheduleChanges(),
          onClick = { onEvent(EditItemEvent.SaveSchedule) },
        ) {
          Text(stringResource(R.string.edit_item_schedule_save))
        }
      }
    }

    item(key = "schedule-enabled") {
      MySwitch(
        modifier = Modifier.animateItem(),
        title = stringResource(R.string.edit_item_schedule_enable),
        checked = uiState.schedule.autoDownloadEpisodes,
        contentDescription = stringResource(R.string.edit_item_schedule_enable),
        enabled = !uiState.isSaving,
        onCheckedChange = { onEvent(EditItemEvent.UpdateScheduleEnabled(it)) },
      )
    }
  }
}

@Composable
private fun ScheduleModeSelector(
  modifier: Modifier = Modifier,
  uiState: EditItemUiState,
  onEvent: (EditItemEvent) -> Unit,
) {
  val simpleLabel = stringResource(R.string.edit_item_schedule_mode_simple)
  val advancedLabel = stringResource(R.string.edit_item_schedule_mode_advanced)
  MySegmentedButton(
    modifier = modifier,
    options = listOf(simpleLabel, advancedLabel),
    selectedValue =
      when (uiState.scheduleMode) {
        PodcastScheduleMode.Simple -> simpleLabel
        PodcastScheduleMode.Advanced -> advancedLabel
      },
    onClick = { selection ->
      onEvent(
        EditItemEvent.ChangeScheduleMode(
          if (selection == simpleLabel) PodcastScheduleMode.Simple else PodcastScheduleMode.Advanced
        )
      )
    },
  )
}

private fun LazyListScope.simpleScheduleItems(
  uiState: EditItemUiState,
  onEvent: (EditItemEvent) -> Unit,
) {
  item(key = "schedule-interval") {
    IntervalDropdown(
      modifier = Modifier.animateItem(),
      uiState = uiState,
      onEvent = onEvent,
    )
  }

  if (uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Custom) {
    item(key = "schedule-weekdays-title") {
      Text(
        modifier = Modifier.animateItem(),
        text = stringResource(R.string.edit_item_schedule_weekdays),
        style = MaterialTheme.typography.titleSmall,
      )
    }

    WEEKDAY_OPTIONS.forEach { (weekday, labelRes) ->
      item(key = "schedule-weekday-$weekday") {
        CheckboxRow(
          modifier = Modifier.animateItem(),
          checked = weekday in uiState.simpleScheduleBuilder.selectedWeekdays,
          text = stringResource(labelRes),
          onCheckedChange = { onEvent(EditItemEvent.ToggleSimpleScheduleWeekday(weekday)) },
          enabled = !uiState.isSaving,
        )
      }
    }
  }

  if (
    uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Custom ||
      uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Daily
  ) {
    item(key = "schedule-time") {
      TimePickerTextField(
        modifier = Modifier.animateItem(),
        enabled = !uiState.isSaving,
        label = stringResource(R.string.edit_item_schedule_time),
        selectedHour = uiState.simpleScheduleBuilder.selectedHour.toIntOrNull() ?: 0,
        selectedMinute = uiState.simpleScheduleBuilder.selectedMinute.toIntOrNull() ?: 0,
        onTimeSelected = { hour, minute ->
          onEvent(EditItemEvent.UpdateSimpleScheduleHour(hour.toString()))
          onEvent(EditItemEvent.UpdateSimpleScheduleMinute(minute.toString()))
        },
      )
    }
  }

  uiState.simpleScheduleGuidance()?.let {
    item(key = "schedule-guidance") {
      Text(
        modifier = Modifier.animateItem(),
        text = it,
        style = MaterialTheme.typography.bodyMedium,
        color =
          if (uiState.simpleScheduleBuilder.validationMessage() != null)
            MaterialTheme.colorScheme.error
          else MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun AdvancedScheduleEditor(
  modifier: Modifier = Modifier,
  uiState: EditItemUiState,
  onEvent: (EditItemEvent) -> Unit,
) {
  OutlinedTextField(
    modifier = modifier.fillMaxWidth(),
    enabled = !uiState.isSaving,
    value = uiState.schedule.cronExpression,
    onValueChange = { onEvent(EditItemEvent.UpdateScheduleCronExpression(it)) },
    label = { Text(stringResource(R.string.edit_item_schedule_cron_expression)) },
    placeholder = { Text(stringResource(R.string.edit_item_schedule_cron_hint)) },
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
    isError = uiState.scheduleCronError != null,
    supportingText = {
      uiState.scheduleCronError?.let {
        Text(
          text = it,
          color = MaterialTheme.colorScheme.error,
        )
      }
    },
  )
}

@Composable
private fun IntervalDropdown(
  modifier: Modifier = Modifier,
  uiState: EditItemUiState,
  onEvent: (EditItemEvent) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    ExposedDropdownMenuBox(
      expanded = expanded,
      onExpandedChange = { expanded = it && !uiState.isSaving },
    ) {
      OutlinedTextField(
        value = uiState.simpleScheduleBuilder.interval.label(),
        onValueChange = {},
        readOnly = true,
        enabled = !uiState.isSaving,
        label = { Text(stringResource(R.string.edit_item_schedule_interval)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier =
          Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      )
      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
      ) {
        PodcastScheduleSimpleInterval.entries.forEach { interval ->
          DropdownMenuItem(
            text = { Text(interval.label()) },
            onClick = {
              onEvent(EditItemEvent.UpdateSimpleScheduleInterval(interval))
              expanded = false
            },
          )
        }
      }
    }
  }
}

@Composable
private fun PodcastScheduleSimpleInterval.label(): String =
  when (this) {
    PodcastScheduleSimpleInterval.Custom ->
      stringResource(R.string.edit_item_schedule_interval_custom)

    PodcastScheduleSimpleInterval.Daily ->
      stringResource(R.string.edit_item_schedule_interval_daily)

    PodcastScheduleSimpleInterval.Every12Hours ->
      stringResource(R.string.edit_item_schedule_interval_every_12_hours)

    PodcastScheduleSimpleInterval.Every6Hours ->
      stringResource(R.string.edit_item_schedule_interval_every_6_hours)

    PodcastScheduleSimpleInterval.Every2Hours ->
      stringResource(R.string.edit_item_schedule_interval_every_2_hours)

    PodcastScheduleSimpleInterval.EveryHour ->
      stringResource(R.string.edit_item_schedule_interval_every_hour)

    PodcastScheduleSimpleInterval.Every30Minutes ->
      stringResource(R.string.edit_item_schedule_interval_every_30_minutes)

    PodcastScheduleSimpleInterval.Every15Minutes ->
      stringResource(R.string.edit_item_schedule_interval_every_15_minutes)
  }

@Composable
private fun MissingRssWarningCard() {
  SectionCard {
    Text(
      text = stringResource(R.string.edit_item_schedule_missing_rss_title),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      text = stringResource(R.string.edit_item_schedule_missing_rss_message),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ScheduleTabPreview() {
  AnimatedPreviewWrapper { ScheduleTab(uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE) }
}

@ShelfDroidPreview
@Composable
private fun ScheduleTabDisabledPreview() {
  AnimatedPreviewWrapper {
    ScheduleTab(
      uiState =
        Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(
          schedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.schedule.copy(autoDownloadEpisodes = false),
          originalSchedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.originalSchedule.copy(autoDownloadEpisodes = false),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ScheduleTabMissingRssPreview() {
  AnimatedPreviewWrapper {
    ScheduleTab(
      uiState =
        Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(
          details = Defaults.EDIT_ITEM_PODCAST_UI_STATE.details.copy(rssFeedUrl = ""),
          originalDetails = Defaults.EDIT_ITEM_PODCAST_UI_STATE.details.copy(rssFeedUrl = ""),
          schedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.schedule.copy(autoDownloadEpisodes = false),
          originalSchedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.originalSchedule.copy(autoDownloadEpisodes = false),
        )
    )
  }
}

private val WEEKDAY_OPTIONS =
  listOf(
    0 to R.string.edit_item_schedule_sunday,
    1 to R.string.edit_item_schedule_monday,
    2 to R.string.edit_item_schedule_tuesday,
    3 to R.string.edit_item_schedule_wednesday,
    4 to R.string.edit_item_schedule_thursday,
    5 to R.string.edit_item_schedule_friday,
    6 to R.string.edit_item_schedule_saturday,
  )
