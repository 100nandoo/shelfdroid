package dev.halim.shelfdroid.core.ui.player.bookmark

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog

@Composable
fun DeleteBookmarkDialog(showDialog: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
  MyAlertDialog(
    title = stringResource(R.string.delete_bookmark_title),
    text = stringResource(R.string.are_you_sure_delete_bookmark),
    showDialog = showDialog,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = { onConfirm() },
    onDismiss = { onDismiss() },
  )
}

@Composable
fun UpdateBookmarkDialog(
  showDialog: Boolean,
  title: String,
  bookmarkTitle: String,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var textFieldValue by
    remember(bookmarkTitle) {
      mutableStateOf(TextFieldValue(bookmarkTitle, TextRange(0, bookmarkTitle.length)))
    }

  if (showDialog) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(title) },
      text = {
        OutlinedTextField(
          value = textFieldValue,
          onValueChange = { textFieldValue = it },
          label = { Text(title) },
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          singleLine = true,
          modifier = Modifier.focusRequester(focusRequester),
        )
      },
      confirmButton = {
        TextButton(onClick = { onConfirm(textFieldValue.text) }) {
          Text(stringResource(R.string.ok))
        }
      },
      dismissButton = {
        TextButton(onClick = { onDismiss() }) { Text(stringResource(R.string.cancel)) }
      },
    )
  }
}
