package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R

@Composable
fun MyAlertDialog(
  modifier: Modifier = Modifier,
  showDialog: Boolean,
  title: String,
  text: String,
  confirmText: String,
  dismissText: String = stringResource(R.string.cancel),
  onConfirm: () -> Unit,
  onDismiss: () -> Unit = {},
) {
  if (showDialog) {
    AlertDialog(
      modifier = modifier,
      onDismissRequest = { onDismiss() },
      title = {
        Text(text = title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
      },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { onConfirm() }) { Text(confirmText) } },
      dismissButton = { TextButton(onClick = { onDismiss() }) { Text(dismissText) } },
    )
  }
}

@Composable
fun MyAlertDialogWithCheckbox(
  modifier: Modifier = Modifier,
  showDialog: Boolean,
  title: String,
  text: String,
  confirmText: String,
  dismissText: String = stringResource(R.string.cancel),
  onConfirm: () -> Unit,
  onDismiss: () -> Unit = {},
  checkboxChecked: Boolean,
  onCheckboxChange: (Boolean) -> Unit,
  checkboxText: String,
) {
  if (showDialog) {
    AlertDialog(
      modifier = modifier,
      onDismissRequest = { onDismiss() },
      title = {
        Text(text = title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
      },
      text = {
        Column {
          Text(text)
          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Checkbox(checked = checkboxChecked, onCheckedChange = onCheckboxChange)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = checkboxText)
          }
        }
      },
      confirmButton = { TextButton(onClick = { onConfirm() }) { Text(confirmText) } },
      dismissButton = { TextButton(onClick = { onDismiss() }) { Text(dismissText) } },
    )
  }
}
