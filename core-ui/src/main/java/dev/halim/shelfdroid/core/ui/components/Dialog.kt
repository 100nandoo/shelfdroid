package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
