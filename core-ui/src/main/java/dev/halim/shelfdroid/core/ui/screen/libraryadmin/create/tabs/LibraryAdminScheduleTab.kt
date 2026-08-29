@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.nextLibraryScheduleRun
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateEvent
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.createErrorText

@Composable
internal fun LibraryAdminScheduleTab(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  val schedule = uiState.draft.schedule
  val scheduleError = uiState.validation.errors[LibraryAdminCreateField.SCHEDULE]
  val errorDescription = scheduleError?.let { createErrorText(it) }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .focusRequester(focusRequester)
        .focusable(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_schedule_heading))
    MySwitch(
      title = stringResource(R.string.library_schedule_enable),
      checked = schedule.enabled,
      contentDescription = stringResource(R.string.library_schedule_enable),
      onCheckedChange = { onEvent(LibraryAdminCreateEvent.ToggleSchedule(it)) },
    )
    if (!schedule.enabled) {
      val disabledDescription = stringResource(R.string.library_schedule_disabled_note)
      Text(
        text = disabledDescription,
        modifier = Modifier.semantics { contentDescription = disabledDescription },
      )
    } else {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = schedule.mode == LibraryAdminScheduleMode.Simple,
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Simple))
          },
          label = { Text(stringResource(R.string.library_schedule_mode_simple)) },
        )
        FilterChip(
          selected = schedule.mode == LibraryAdminScheduleMode.Advanced,
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced))
          },
          label = { Text(stringResource(R.string.library_schedule_mode_advanced)) },
        )
      }

      when (schedule.mode) {
        LibraryAdminScheduleMode.Simple -> LibraryAdminSimpleScheduleContent(schedule, onEvent)
        LibraryAdminScheduleMode.Advanced ->
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = schedule.advancedCronExpression,
              onValueChange = {
                onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron(it))
              },
              label = { Text(stringResource(R.string.library_schedule_cron_expression)) },
              placeholder = { Text(stringResource(R.string.library_schedule_cron_hint)) },
              modifier = Modifier.fillMaxWidth(),
              isError = scheduleError != null,
              supportingText = {
                when (val validation = uiState.scheduleValidation) {
                  LibraryAdminScheduleValidationState.Validating ->
                    Text(stringResource(R.string.library_schedule_checking))

                  is LibraryAdminScheduleValidationState.Invalid,
                  is LibraryAdminScheduleValidationState.Unavailable ->
                    Text(
                      text = errorDescription.orEmpty(),
                      modifier =
                        Modifier.semantics {
                          contentDescription = errorDescription.orEmpty()
                        },
                    )

                  else ->
                    if (scheduleError != null) {
                      Text(
                        text = errorDescription.orEmpty(),
                        modifier =
                          Modifier.semantics {
                            contentDescription = errorDescription.orEmpty()
                          },
                      )
                    }
                }
              },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Button(
              enabled = !uiState.isBusy,
              onClick = { onEvent(LibraryAdminCreateEvent.ValidateSchedule) },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                if (uiState.scheduleValidation is LibraryAdminScheduleValidationState.Validating) {
                  stringResource(R.string.library_schedule_checking)
                } else {
                  stringResource(R.string.library_schedule_validate)
                }
              )
            }
          }
      }

      if (schedule.mode == LibraryAdminScheduleMode.Simple && scheduleError != null) {
        Text(
          text = errorDescription.orEmpty(),
          modifier = Modifier.semantics { contentDescription = errorDescription.orEmpty() },
        )
      }

      if (
        scheduleError == null &&
          (schedule.mode == LibraryAdminScheduleMode.Simple ||
            uiState.scheduleValidation is LibraryAdminScheduleValidationState.Valid)
      ) {
        ScheduleSummary(schedule.cronExpression, schedule.summary)
      }
    }
  }
}

@Composable
private fun LibraryAdminSimpleScheduleContent(
  schedule: LibraryAdminScheduleDraft,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
  ) {
    OutlinedTextField(
      value = schedule.simple.interval.scheduleLabel(),
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.library_schedule_interval)) },
      modifier =
        Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      LibraryAdminScheduleInterval.entries.forEach { interval ->
        DropdownMenuItem(
          text = { Text(interval.scheduleLabel()) },
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleInterval(interval))
            expanded = false
          },
        )
      }
    }
  }

  if (schedule.simple.interval == LibraryAdminScheduleInterval.Custom) {
    Text(stringResource(R.string.library_schedule_weekdays))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LIBRARY_SCHEDULE_WEEKDAY_OPTIONS.forEach { (day, label) ->
        FilterChip(
          selected = day in schedule.simple.weekdays,
          onClick = {
            onEvent(
              LibraryAdminCreateEvent.ToggleScheduleWeekday(
                weekday = day,
                selected = day !in schedule.simple.weekdays,
              )
            )
          },
          label = { Text(stringResource(label)) },
        )
      }
    }
  }

  if (
    schedule.simple.interval == LibraryAdminScheduleInterval.Custom ||
      schedule.simple.interval == LibraryAdminScheduleInterval.Daily
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = schedule.simple.hour,
        onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateScheduleHour(it)) },
        label = { Text(stringResource(R.string.library_schedule_hour)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f),
        isError = schedule.simple.hour.toIntOrNull() !in 0..23,
      )
      OutlinedTextField(
        value = schedule.simple.minute,
        onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateScheduleMinute(it)) },
        label = { Text(stringResource(R.string.library_schedule_minute)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f),
        isError = schedule.simple.minute.toIntOrNull() !in 0..59,
      )
    }
  }
}

@Composable
private fun ScheduleSummary(expression: String?, summary: String?) {
  if (expression.isNullOrBlank()) return
  val nextRun = remember(expression) { nextLibraryScheduleRun(expression) }
  Column(
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = expression },
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = summary ?: stringResource(R.string.library_schedule_valid),
      modifier = Modifier.fillMaxWidth(),
    )
    Text(text = expression, modifier = Modifier.fillMaxWidth())
    if (nextRun.isNotBlank()) {
      Text(
        text = stringResource(R.string.library_schedule_next_run, nextRun),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun LibraryAdminScheduleInterval.scheduleLabel(): String =
  when (this) {
    LibraryAdminScheduleInterval.Custom -> stringResource(R.string.library_schedule_interval_custom)
    LibraryAdminScheduleInterval.Daily -> stringResource(R.string.library_schedule_interval_daily)
    LibraryAdminScheduleInterval.Every12Hours ->
      stringResource(R.string.library_schedule_interval_every_12_hours)

    LibraryAdminScheduleInterval.Every6Hours ->
      stringResource(R.string.library_schedule_interval_every_6_hours)

    LibraryAdminScheduleInterval.Every2Hours ->
      stringResource(R.string.library_schedule_interval_every_2_hours)

    LibraryAdminScheduleInterval.EveryHour ->
      stringResource(R.string.library_schedule_interval_every_hour)

    LibraryAdminScheduleInterval.Every30Minutes ->
      stringResource(R.string.library_schedule_interval_every_30_minutes)

    LibraryAdminScheduleInterval.Every15Minutes ->
      stringResource(R.string.library_schedule_interval_every_15_minutes)
  }

private val LIBRARY_SCHEDULE_WEEKDAY_OPTIONS =
  listOf(
    0 to R.string.library_schedule_sunday,
    1 to R.string.library_schedule_monday,
    2 to R.string.library_schedule_tuesday,
    3 to R.string.library_schedule_wednesday,
    4 to R.string.library_schedule_thursday,
    5 to R.string.library_schedule_friday,
    6 to R.string.library_schedule_saturday,
  )
