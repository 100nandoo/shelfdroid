package dev.halim.shelfdroid.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun MyAlertDialog(
  modifier: Modifier = Modifier,
  showDialog: Boolean,
  title: String,
  text: String,
  confirmText: String,
  dismissText: String = "Cancel",
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (showDialog) {
    AlertDialog(
      modifier = modifier,
      onDismissRequest = { onDismiss() },
      title = { Text(title) },
      text = { Text(text) },
      confirmButton = { TextButton(onClick = { onConfirm() }) { Text(confirmText) } },
      dismissButton = { TextButton(onClick = { onDismiss() }) { Text(dismissText) } },
    )
  }
}
