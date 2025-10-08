package dev.halim.shelfdroid.core.ui.components

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenu(
  modifier: Modifier = Modifier,
  options: List<String> = emptyList(),
  label: String? = null,
  initialValue: String = "",
  onClick: (String) -> Unit = {},
) {
  var expanded by remember { mutableStateOf(false) }

  var textFieldValue by remember(initialValue) { mutableStateOf(TextFieldValue(initialValue)) }

  ExposedDropdownMenuBox(
    modifier = modifier,
    expanded = expanded,
    onExpandedChange = { expanded = it },
  ) {
    OutlinedTextField(
      modifier = modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
      readOnly = true,
      value = textFieldValue,
      onValueChange = { textFieldValue = it },
      label = { label?.let { Text(it) } },
      singleLine = true,
      maxLines = 1,
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )

    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { selectionOption ->
        DropdownMenuItem(
          text = { Text(text = selectionOption) },
          onClick = {
            textFieldValue = TextFieldValue(selectionOption)
            expanded = false
            onClick(selectionOption)
          },
        )
      }
    }
  }
}
