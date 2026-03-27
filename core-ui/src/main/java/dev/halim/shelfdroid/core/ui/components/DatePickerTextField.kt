@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.ui.R
import java.util.Date

@Composable
fun DatePickerTextField(
  modifier: Modifier = Modifier,
  label: String,
  selectedDateMillis: Long?,
  isError: Boolean = false,
  onDateSelected: (Long) -> Unit,
) {
  var showDatePicker by remember { mutableStateOf(false) }
  val todayMillis = remember {
    val now = System.currentTimeMillis()
    now - now % (24 * 60 * 60 * 1000)
  }
  val currentYear = remember { java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) }
  val datePickerState =
    rememberDatePickerState(
      initialSelectedDateMillis = selectedDateMillis,
      yearRange = currentYear..currentYear + 100,
      selectableDates =
        object : SelectableDates {
          override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis

          override fun isSelectableYear(year: Int): Boolean = year >= currentYear
        },
    )

  val dateText =
    selectedDateMillis?.let { millis ->
      java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG).format(Date(millis))
    } ?: ""

  Box(modifier = modifier) {
    OutlinedTextField(
      value = dateText,
      onValueChange = {},
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth(),
      readOnly = true,
      isError = isError,
      trailingIcon = {
        IconButton(onClick = { showDatePicker = true }) {
          Icon(painter = painterResource(R.drawable.calendar), contentDescription = label)
        }
      },
    )

    Box(modifier = Modifier.matchParentSize().alpha(0f).clickable { showDatePicker = true })
  }

  if (showDatePicker) {
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis -> onDateSelected(millis) }
            showDatePicker = false
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
}
