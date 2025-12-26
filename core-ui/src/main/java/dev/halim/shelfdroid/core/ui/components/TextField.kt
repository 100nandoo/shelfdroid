package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun MyOutlinedTextField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  prefix: String? = null,
  placeholder: String? = null,
  keyboardOptions: KeyboardOptions,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onNext: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = { onValueChange(it) },
    label = { Text(label) },
    prefix = prefix?.let { { Text(it, color = MaterialTheme.colorScheme.primary) } },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation = visualTransformation,
    modifier = modifier.fillMaxWidth(),
    keyboardActions =
      KeyboardActions(onNext = onNext?.let { { it() } }, onDone = onDone?.let { { it() } }),
  )
}

@Composable
fun MyOutlinedTextField(
  modifier: Modifier = Modifier,
  value: TextFieldValue,
  onValueChange: (TextFieldValue) -> Unit,
  label: String,
  placeholder: String? = null,
  keyboardOptions: KeyboardOptions,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onNext: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,
  singleLine: Boolean = true,
  maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
  prefix: @Composable (() -> Unit)? = null,
  supportingText: @Composable (() -> Unit)? = null,
) {
  var isFocused by remember { mutableStateOf(false) }

  LaunchedEffect(isFocused) {
    if (isFocused) {
      onValueChange(value.copy(selection = TextRange(value.text.length)))
    }
  }

  OutlinedTextField(
    value = value,
    onValueChange = { onValueChange(it) },
    label = { Text(label) },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation = visualTransformation,
    modifier = modifier.fillMaxWidth().onFocusChanged { state -> isFocused = state.isFocused },
    prefix = prefix,
    keyboardActions =
      KeyboardActions(onNext = onNext?.let { { it() } }, onDone = onDone?.let { { it() } }),
    singleLine = singleLine,
    maxLines = maxLines,
    supportingText = supportingText,
  )
}
