@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun FilterDialog(
  showDialog: Boolean,
  filterState: AddEpisodeFilterState = AddEpisodeFilterState(),
  onConfirm: () -> Unit,
  onDismiss: () -> Unit = {},
  onEvent: (AddEpisodeEvent.FilterEvent) -> Unit = {},
) {
  var showPublishedDatePicker by remember { mutableStateOf(false) }

  PublishedDateRangePickerDialog(
    showDialog = showPublishedDatePicker,
    startDateMillis = filterState.publishedStartDateMillis,
    endDateMillis = filterState.publishedEndDateMillis,
    onDismiss = { showPublishedDatePicker = false },
    onDateRangeSelected = { startDateMillis, endDateMillis ->
      onEvent(AddEpisodeEvent.FilterEvent.PublishedDateRangeChanged(startDateMillis, endDateMillis))
      showPublishedDatePicker = false
    },
  )

  if (showDialog) {
    AlertDialog(
      onDismissRequest = { onDismiss() },
      title = {
        Text(
          text = stringResource(R.string.filter),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      text = {
        Column {
          MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filterState.titleQuery,
            onValueChange = { onEvent(AddEpisodeEvent.FilterEvent.TitleChanged(it)) },
            label = stringResource(R.string.title),
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          )
          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = stringResource(R.string.published_date),
            style = MaterialTheme.typography.titleMedium,
          )
          Spacer(modifier = Modifier.height(8.dp))
          FilterDateField(
            label = stringResource(R.string.from),
            value = filterState.publishedStartDateMillis?.toPickerDateLabel().orEmpty(),
            onClick = { showPublishedDatePicker = true },
            onClear = {
              onEvent(
                AddEpisodeEvent.FilterEvent.PublishedDateRangeChanged(
                  startDateMillis = null,
                  endDateMillis = filterState.publishedEndDateMillis,
                )
              )
            },
          )
          Spacer(modifier = Modifier.height(8.dp))
          FilterDateField(
            label = stringResource(R.string.to),
            value = filterState.publishedEndDateMillis?.toPickerDateLabel().orEmpty(),
            onClick = { showPublishedDatePicker = true },
            onClear = {
              onEvent(
                AddEpisodeEvent.FilterEvent.PublishedDateRangeChanged(
                  startDateMillis = filterState.publishedStartDateMillis,
                  endDateMillis = null,
                )
              )
            },
          )

          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Checkbox(
              checked = filterState.hideDownloaded,
              onCheckedChange = { onEvent(AddEpisodeEvent.FilterEvent.HideDownloadedChanged(it)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.hide_downloaded_episode))
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { onConfirm() }) { Text(stringResource(R.string.ok)) }
      },
      dismissButton = {
        Row {
          TextButton(onClick = { onEvent(AddEpisodeEvent.FilterEvent.ResetFilters) }) {
            Text(stringResource(R.string.clear))
          }
          TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.cancel)) }
        }
      },
    )
  }
}

@Composable
private fun FilterDateField(
  label: String,
  value: String,
  onClick: () -> Unit,
  onClear: () -> Unit,
) {
  OutlinedTextField(
    value = value,
    onValueChange = {},
    label = { Text(label) },
    modifier = Modifier.fillMaxWidth().clickable { onClick() },
    readOnly = true,
    trailingIcon = {
      IconButton(onClick = if (value.isBlank()) onClick else onClear) {
        Icon(
          painter = painterResource(if (value.isBlank()) R.drawable.calendar else R.drawable.close),
          contentDescription = if (value.isBlank()) label else stringResource(R.string.clear),
        )
      }
    },
  )
}

@Composable
private fun PublishedDateRangePickerDialog(
  showDialog: Boolean,
  startDateMillis: Long?,
  endDateMillis: Long?,
  onDismiss: () -> Unit,
  onDateRangeSelected: (Long?, Long?) -> Unit,
) {
  if (!showDialog) return

  val dateRangePickerState =
    rememberDateRangePickerState(
      initialSelectedStartDateMillis = startDateMillis,
      initialSelectedEndDateMillis = if (startDateMillis != null) endDateMillis else null,
      initialDisplayedMonthMillis = startDateMillis ?: endDateMillis,
    )

  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      TextButton(
        onClick = {
          val selectedStartDateMillis = dateRangePickerState.selectedStartDateMillis
          val selectedEndDateMillis = dateRangePickerState.selectedEndDateMillis
          val preservedEndOnlySelection =
            selectedStartDateMillis == null &&
              selectedEndDateMillis == null &&
              startDateMillis == null &&
              endDateMillis != null

          onDateRangeSelected(
            if (preservedEndOnlySelection) null else selectedStartDateMillis,
            if (preservedEndOnlySelection) endDateMillis else selectedEndDateMillis,
          )
        }
      ) {
        Text(stringResource(R.string.submit))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  ) {
    DateRangePicker(
      state = dateRangePickerState,
      modifier = Modifier.padding(top = 8.dp),
      title = { PublishedDateRangePickerTitle() },
      headline = {
        PublishedDateRangePickerHeadline(
          startDateMillis = dateRangePickerState.selectedStartDateMillis,
          endDateMillis = dateRangePickerState.selectedEndDateMillis,
        )
      },
      showModeToggle = false,
    )
  }
}

@Composable
private fun PublishedDateRangePickerTitle() {
  Text(
    text = stringResource(R.string.select_dates),
    style = MaterialTheme.typography.labelLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
  )
}

@Composable
private fun PublishedDateRangePickerHeadline(startDateMillis: Long?, endDateMillis: Long?) {
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PublishedDateRangeHeadlineLine(
      label = stringResource(R.string.from),
      value = startDateMillis?.toPickerHeadlineDateLabel() ?: stringResource(R.string.not_set),
    )
    PublishedDateRangeHeadlineLine(
      label = stringResource(R.string.to),
      value = endDateMillis?.toPickerHeadlineDateLabel() ?: stringResource(R.string.not_set),
    )
  }
}

@Composable
private fun PublishedDateRangeHeadlineLine(label: String, value: String) {
  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
      text = value,
      style = MaterialTheme.typography.titleLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

private fun Long.toPickerDateLabel(): String {
  val formatter =
    SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
  return formatter.format(Date(this))
}

private fun Long.toPickerHeadlineDateLabel(): String {
  val formatter =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).apply {
      timeZone = TimeZone.getTimeZone("UTC")
    }
  return formatter.format(Date(this))
}

private const val PreviewPublishedStartDateMillis = 1_717_200_000_000L
private const val PreviewPublishedEndDateMillis = 1_717_372_800_000L

@ShelfDroidPreview
@Composable
fun FilterDialogPreview() {
  AnimatedPreviewWrapper { FilterDialog(showDialog = true, onConfirm = {}, onDismiss = {}) }
}

@ShelfDroidPreview
@Composable
private fun PublishedDateRangePickerDialogPreview() {
  AnimatedPreviewWrapper {
    PublishedDateRangePickerDialog(
      showDialog = true,
      startDateMillis = PreviewPublishedStartDateMillis,
      endDateMillis = PreviewPublishedEndDateMillis,
      onDismiss = {},
      onDateRangeSelected = { _, _ -> },
    )
  }
}
