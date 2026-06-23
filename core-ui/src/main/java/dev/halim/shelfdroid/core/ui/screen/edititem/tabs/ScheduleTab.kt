package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun ScheduleTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit = {}) {
  LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    if (!uiState.canConfigureSchedule()) {
      item { MissingRssWarningCard() }
      return@LazyColumn
    }

    item {
      SectionCard {
        MySwitch(
          title = stringResource(R.string.edit_item_schedule_enable),
          checked = uiState.schedule.autoDownloadEpisodes,
          contentDescription = stringResource(R.string.edit_item_schedule_enable),
          enabled = !uiState.isSaving,
          onCheckedChange = { onEvent(EditItemEvent.UpdateScheduleEnabled(it)) },
        )

        if (uiState.schedule.autoDownloadEpisodes) {
          MyOutlinedTextField(
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
          MyOutlinedTextField(
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

          ScheduleModeSelector(uiState = uiState, onEvent = onEvent)

          when (uiState.scheduleMode) {
            PodcastScheduleMode.Simple -> SimpleScheduleEditor(uiState = uiState, onEvent = onEvent)
            PodcastScheduleMode.Advanced ->
              AdvancedScheduleEditor(uiState = uiState, onEvent = onEvent)
          }
        } else {
          Text(
            text = stringResource(R.string.edit_item_schedule_disabled_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        Button(
          modifier = Modifier.align(Alignment.End),
          enabled = !uiState.isSaving && uiState.hasScheduleChanges(),
          onClick = { onEvent(EditItemEvent.SaveSchedule) },
        ) {
          Text(stringResource(R.string.edit_item_schedule_save))
        }
      }
    }
  }
}

@Composable
private fun ScheduleModeSelector(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val simpleLabel = stringResource(R.string.edit_item_schedule_mode_simple)
  val advancedLabel = stringResource(R.string.edit_item_schedule_mode_advanced)
  MySegmentedButton(
    options = listOf(simpleLabel, advancedLabel),
    selectedValue =
      when (uiState.scheduleMode) {
        PodcastScheduleMode.Simple -> simpleLabel
        PodcastScheduleMode.Advanced -> advancedLabel
      },
    onClick = { selection ->
      onEvent(
        EditItemEvent.ChangeScheduleMode(
          if (selection == simpleLabel) PodcastScheduleMode.Simple
          else PodcastScheduleMode.Advanced
        )
      )
    },
  )
}

@Composable
private fun SimpleScheduleEditor(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      text = stringResource(R.string.edit_item_schedule_interval),
      style = MaterialTheme.typography.titleMedium,
    )

    PodcastScheduleSimpleInterval.entries.forEach { interval ->
      IntervalOptionRow(
        selected = uiState.simpleScheduleBuilder.interval == interval,
        label = interval.label(),
        enabled = !uiState.isSaving,
        onClick = { onEvent(EditItemEvent.UpdateSimpleScheduleInterval(interval)) },
      )
    }

    if (uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Custom) {
      Text(
        text = stringResource(R.string.edit_item_schedule_weekdays),
        style = MaterialTheme.typography.titleSmall,
      )
      WEEKDAY_OPTIONS.forEach { (weekday, labelRes) ->
        CheckboxRow(
          checked = weekday in uiState.simpleScheduleBuilder.selectedWeekdays,
          text = stringResource(labelRes),
          onCheckedChange = { onEvent(EditItemEvent.ToggleSimpleScheduleWeekday(weekday)) },
          enabled = !uiState.isSaving,
        )
      }
    }

    if (
      uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Custom ||
        uiState.simpleScheduleBuilder.interval == PodcastScheduleSimpleInterval.Daily
    ) {
      TimeInputFields(uiState = uiState, onEvent = onEvent)
    }

    uiState.simpleScheduleGuidance()?.let {
      Text(
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
private fun AdvancedScheduleEditor(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  OutlinedTextField(
    modifier = Modifier.fillMaxWidth(),
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
private fun TimeInputFields(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  MyOutlinedTextField(
    enabled = !uiState.isSaving,
    value = uiState.simpleScheduleBuilder.selectedHour,
    onValueChange = { onEvent(EditItemEvent.UpdateSimpleScheduleHour(it)) },
    label = stringResource(R.string.edit_item_schedule_hour),
    placeholder = stringResource(R.string.edit_item_schedule_hour_hint),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
  )
  MyOutlinedTextField(
    enabled = !uiState.isSaving,
    value = uiState.simpleScheduleBuilder.selectedMinute,
    onValueChange = { onEvent(EditItemEvent.UpdateSimpleScheduleMinute(it)) },
    label = stringResource(R.string.edit_item_schedule_minute),
    placeholder = stringResource(R.string.edit_item_schedule_minute_hint),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
  )
}

@Composable
private fun IntervalOptionRow(
  selected: Boolean,
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(selected = selected, onClick = onClick, enabled = enabled)
    Text(
      text = label,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
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
  PreviewWrapper { ScheduleTab(uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE) }
}

@ShelfDroidPreview
@Composable
private fun ScheduleTabDisabledPreview() {
  PreviewWrapper {
    ScheduleTab(
      uiState =
        Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(
          schedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.schedule.copy(autoDownloadEpisodes = false),
          originalSchedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.originalSchedule.copy(
              autoDownloadEpisodes = false
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ScheduleTabMissingRssPreview() {
  PreviewWrapper {
    ScheduleTab(
      uiState =
        Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(
          details = Defaults.EDIT_ITEM_PODCAST_UI_STATE.details.copy(rssFeedUrl = ""),
          originalDetails = Defaults.EDIT_ITEM_PODCAST_UI_STATE.details.copy(rssFeedUrl = ""),
          schedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.schedule.copy(autoDownloadEpisodes = false),
          originalSchedule =
            Defaults.EDIT_ITEM_PODCAST_UI_STATE.originalSchedule.copy(
              autoDownloadEpisodes = false
            ),
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
