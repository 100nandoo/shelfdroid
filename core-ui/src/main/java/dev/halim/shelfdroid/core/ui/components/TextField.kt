@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MyOutlinedTextField(
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
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
  val prefixColor =
    if (enabled) OutlinedTextFieldDefaults.colors().unfocusedTextColor
    else OutlinedTextFieldDefaults.colors().disabledTextColor
  OutlinedTextField(
    modifier = modifier.fillMaxWidth(),
    readOnly = enabled.not(),
    enabled = enabled,
    value = value,
    onValueChange = { onValueChange(it) },
    label = { Text(label) },
    prefix = prefix?.let { { Text(it, color = prefixColor) } },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation = visualTransformation,
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

@Composable
fun PasswordTextField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String? = null,
  keyboardOptions: KeyboardOptions,
  onNext: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,
) {
  var passwordVisible by remember { mutableStateOf(false) }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation =
      if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
      val description = if (passwordVisible) "Hide password" else "Show password"

      IconButton(onClick = { passwordVisible = !passwordVisible }) {
        Icon(imageVector = image, contentDescription = description)
      }
    },
    modifier = modifier.fillMaxWidth(),
    keyboardActions =
      KeyboardActions(onNext = onNext?.let { { it() } }, onDone = onDone?.let { { it() } }),
  )
}

@Composable
fun DropdownOutlinedTextField(
  modifier: Modifier = Modifier,
  selectedOptions: List<String>,
  onOptionToggled: (String) -> Unit,
  onOptionRemoved: (String) -> Unit,
  label: String,
  options: List<String>,
  placeholder: String? = null,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = it },
    modifier = modifier,
  ) {
    OutlinedTextField(
      value = if (selectedOptions.isEmpty()) "" else " ",
      onValueChange = {},
      readOnly = true,
      label = { Text(label) },
      placeholder = if (selectedOptions.isEmpty()) placeholder?.let { { Text(it) } } else null,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      prefix =
        if (selectedOptions.isNotEmpty()) {
          {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              selectedOptions.forEach { option ->
                InputChip(
                  selected = true,
                  onClick = {},
                  label = { Text(option) },
                  trailingIcon = {
                    IconButton(
                      modifier = Modifier.size(18.dp),
                      onClick = { onOptionRemoved(option) },
                    ) {
                      Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Remove $option",
                        modifier = Modifier.size(InputChipDefaults.IconSize),
                      )
                    }
                  },
                )
              }
            }
          }
        } else {
          null
        },
      modifier =
        Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        val isSelected = option in selectedOptions
        DropdownMenuItem(
          text = { Text(option) },
          onClick = { onOptionToggled(option) },
          trailingIcon =
            if (isSelected) {
              { Icon(painter = painterResource(R.drawable.check), contentDescription = "Selected") }
            } else {
              null
            },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun DropdownOutlinedTextFieldEmptyPreview() {
  PreviewWrapper {
    DropdownOutlinedTextField(
      selectedOptions = emptyList(),
      onOptionToggled = {},
      onOptionRemoved = {},
      label = "Select Tags",
      options = listOf("Fiction", "Science", "History", "Fantasy"),
      placeholder = "Choose tags...",
    )
  }
}

@ShelfDroidPreview
@Composable
fun DropdownOutlinedTextFieldSelectedPreview() {
  val options = listOf("Fiction", "Science", "History", "Fantasy")
  PreviewWrapper {
    DropdownOutlinedTextField(
      selectedOptions = options,
      onOptionToggled = {},
      onOptionRemoved = {},
      label = "Select Tags",
      options = options,
    )
  }
}
