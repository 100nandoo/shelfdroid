@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.R as CoreR
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import java.util.Locale

@Composable
fun TimePickerTextField(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  label: String,
  selectedHour: Int,
  selectedMinute: Int,
  onTimeSelected: (hour: Int, minute: Int) -> Unit,
) {
  var showTimePicker by remember { mutableStateOf(false) }
  val hour = selectedHour.coerceIn(0, 23)
  val minute = selectedMinute.coerceIn(0, 59)

  Box(modifier = modifier) {
    OutlinedTextField(
      value = formatTimeInput(hour = hour, minute = minute),
      onValueChange = {},
      label = { Text(label) },
      modifier = Modifier.fillMaxWidth(),
      readOnly = true,
      enabled = enabled,
      trailingIcon = {
        IconButton(enabled = enabled, onClick = { showTimePicker = true }) {
          Icon(painter = painterResource(CoreR.drawable.timer), contentDescription = label)
        }
      },
    )

    if (enabled) {
      Box(modifier = Modifier.matchParentSize().alpha(0f).clickable { showTimePicker = true })
    }
  }

  if (showTimePicker) {
    val timePickerState =
      rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true,
      )

    AlertDialog(
      onDismissRequest = { showTimePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            onTimeSelected(timePickerState.hour, timePickerState.minute)
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

private fun formatTimeInput(hour: Int, minute: Int): String =
  String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

@ShelfDroidPreview
@Composable
private fun TimePickerTextFieldPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    TimePickerTextField(
      label = "Schedule time",
      selectedHour = 23,
      selectedMinute = 15,
      onTimeSelected = { _, _ -> },
    )
  }
}
