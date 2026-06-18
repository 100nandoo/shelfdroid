@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun DateTimePickerTextField(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: String,
  selectedDateTimeMillis: Long?,
  placeholder: String? = null,
  onDateTimeSelected: (Long) -> Unit,
) {
  var showDatePicker by remember { mutableStateOf(false) }
  var showTimePicker by remember { mutableStateOf(false) }
  var selectedDateMillis by remember {
    mutableLongStateOf(selectedDateTimeMillis ?: System.currentTimeMillis())
  }
  val value = selectedDateTimeMillis?.let(::formatDateTimeInput).orEmpty()

  Box(modifier = modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      label = { Text(label) },
      placeholder = placeholder?.let { { Text(it) } },
      modifier = Modifier.fillMaxWidth(),
      readOnly = true,
      enabled = enabled,
      trailingIcon = {
        IconButton(enabled = enabled, onClick = { showDatePicker = true }) {
          Icon(painter = painterResource(R.drawable.calendar), contentDescription = label)
        }
      },
    )

    if (enabled) {
      Box(
        modifier =
          Modifier.matchParentSize().alpha(0f).clickable {
            selectedDateMillis = selectedDateTimeMillis ?: System.currentTimeMillis()
            showDatePicker = true
          }
      )
    }
  }

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)

    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            selectedDateMillis = datePickerState.selectedDateMillis ?: selectedDateMillis
            showDatePicker = false
            showTimePicker = true
          }
        ) {
          Text(stringResource(R.string.submit))
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showTimePicker) {
    val calendar =
      remember(selectedDateMillis) {
        Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
      }
    val timePickerState =
      rememberTimePickerState(
        initialHour = calendar.get(Calendar.HOUR_OF_DAY),
        initialMinute = calendar.get(Calendar.MINUTE),
        is24Hour = false,
      )

    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            val updatedCalendar =
              Calendar.getInstance().apply {
                timeInMillis = selectedDateMillis
                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                set(Calendar.MINUTE, timePickerState.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
              }
            onDateTimeSelected(updatedCalendar.timeInMillis)
            showTimePicker = false
          }
        ) {
          Text(stringResource(R.string.submit))
        }
      },
      dismissButton = {
        TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) }
      },
      text = { TimePicker(state = timePickerState) },
    )
  }
}

private fun formatDateTimeInput(value: Long): String =
  SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
    .apply {
      timeZone = TimeZone.getDefault()
    }
    .format(value)

@ShelfDroidPreview
@Composable
private fun DateTimePickerTextFieldPreview() {
  PreviewWrapper(dynamicColor = false) {
    DateTimePickerTextField(
      label = "Episode update cutoff",
      selectedDateTimeMillis = 1781818200000L,
      placeholder = "YYYY-MM-DD hh:mm AM/PM",
      onDateTimeSelected = {},
    )
  }
}
