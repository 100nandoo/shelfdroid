package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChipDropdownMenu(
  modifier: Modifier = Modifier,
  options: List<String> = emptyList(),
  label: String? = null,
  initialValue: String,
  onClick: (String) -> Unit = {},
) {
  var expanded by remember { mutableStateOf(false) }
  var selected by remember(initialValue) { mutableStateOf(initialValue) }

  ExposedDropdownMenuBox(
    modifier = modifier,
    expanded = expanded,
    onExpandedChange = { expanded = it },
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (label != null) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = OutlinedTextFieldDefaults.colors().unfocusedLabelColor,
          modifier = Modifier.padding(end = 8.dp),
        )
      }
      FilterChip(
        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        selected = expanded,
        onClick = {},
        label = { Text(text = selected.ifEmpty { label ?: "" }) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
      )
    }

    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      options.forEach { option ->
        DropdownMenuItem(
          text = { Text(option) },
          onClick = {
            selected = option
            expanded = false
            onClick(option)
          },
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun ChipDropdownMenuPreview() {
  PreviewWrapper {
    ChipDropdownMenu(
      options = listOf("Option 1", "Option 2", "Option 3"),
      label = "Label",
      initialValue = "Option 1",
    )
  }
}
