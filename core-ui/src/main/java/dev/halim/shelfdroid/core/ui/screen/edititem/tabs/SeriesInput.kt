package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.SeriesEntry
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SeriesInput(
  label: String,
  values: List<SeriesEntry>,
  suggestions: List<String>,
  onAdd: (SeriesEntry) -> Unit,
  onRemove: (SeriesEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showDialog by remember { mutableStateOf(false) }

  Column(modifier = modifier.fillMaxWidth()) {
    Text(label)
    if (values.isNotEmpty()) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { entry ->
          val chipLabel =
            if (entry.sequence.isNotBlank()) "${entry.name} #${entry.sequence}" else entry.name
          InputChip(
            selected = true,
            onClick = {},
            label = { Text(chipLabel) },
            trailingIcon = {
              IconButton(
                modifier = Modifier.size(InputChipDefaults.IconSize),
                onClick = { onRemove(entry) },
              ) {
                Icon(
                  painter = painterResource(R.drawable.close),
                  contentDescription = stringResource(R.string.edit_item_remove_chip, chipLabel),
                )
              }
            },
          )
        }
      }
    }

    OutlinedTextField(
      value = "",
      onValueChange = {},
      readOnly = true,
      placeholder = { Text(stringResource(R.string.edit_item_add_chip, label)) },
      modifier = Modifier.fillMaxWidth(),
      trailingIcon = {
        IconButton(onClick = { showDialog = true }) {
          Icon(
            painter = painterResource(R.drawable.add),
            contentDescription = stringResource(R.string.edit_item_series_add),
          )
        }
      },
    )
  }

  if (showDialog) {
    SeriesDialog(
      suggestions = suggestions - values.map { it.name }.toSet(),
      onDismiss = { showDialog = false },
      onSubmit = { entry ->
        onAdd(entry)
        showDialog = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeriesDialog(
  suggestions: List<String>,
  onDismiss: () -> Unit,
  onSubmit: (SeriesEntry) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var sequence by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(stringResource(R.string.edit_item_series_add)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val trimmedName = name.trim()
        val filteredSuggestions =
          remember(suggestions, trimmedName) {
            suggestions.filter {
              trimmedName.isBlank() || it.contains(trimmedName, ignoreCase = true)
            }
          }
        var nameFocused by remember { mutableStateOf(false) }
        val showSuggestions = nameFocused && filteredSuggestions.isNotEmpty()

        ExposedDropdownMenuBox(
          expanded = showSuggestions,
          onExpandedChange = {},
          modifier = Modifier.fillMaxWidth(),
        ) {
          OutlinedTextField(
            value = name,
            onValueChange = {
              name = it
              nameFocused = true
            },
            label = { Text(stringResource(R.string.edit_item_series_name)) },
            singleLine = true,
            modifier =
              Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          )
          ExposedDropdownMenu(
            expanded = showSuggestions,
            onDismissRequest = { nameFocused = false },
          ) {
            filteredSuggestions.forEach { suggestion ->
              DropdownMenuItem(
                text = { Text(suggestion) },
                onClick = {
                  name = suggestion
                  nameFocused = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
              )
            }
          }
        }

        MyOutlinedTextField(
          value = sequence,
          onValueChange = { sequence = it },
          label = stringResource(R.string.edit_item_series_sequence),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        )
      }
    },
    confirmButton = {
      TextButton(
        enabled = name.isNotBlank(),
        onClick = { onSubmit(SeriesEntry(name.trim(), sequence.trim())) },
      ) {
        Text(stringResource(R.string.submit))
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
  )
}

@ShelfDroidPreview
@Composable
private fun SeriesInputPreview() {
  PreviewWrapper {
    SeriesInput(
      label = "Series",
      values = listOf(SeriesEntry("The Lord of the Rings", "1")),
      suggestions = listOf("Foundation", "Dune"),
      onAdd = {},
      onRemove = {},
    )
  }
}
