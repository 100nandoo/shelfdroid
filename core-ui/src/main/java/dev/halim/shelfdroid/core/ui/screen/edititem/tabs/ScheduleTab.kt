package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.canConfigureSchedule
import dev.halim.shelfdroid.core.data.screen.edititem.hasScheduleChanges
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
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
          OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving,
            value = uiState.schedule.cronExpression,
            onValueChange = { onEvent(EditItemEvent.UpdateScheduleCronExpression(it)) },
            label = { Text(stringResource(R.string.edit_item_schedule_cron_expression)) },
            placeholder = { Text(stringResource(R.string.edit_item_schedule_cron_hint)) },
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
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
