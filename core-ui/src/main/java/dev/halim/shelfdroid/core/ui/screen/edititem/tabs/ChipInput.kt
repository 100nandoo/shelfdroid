package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChipInput(
  label: String,
  values: List<String>,
  onAdd: (String) -> Unit,
  onRemove: (String) -> Unit,
  modifier: Modifier = Modifier,
  suggestions: List<String> = emptyList(),
) {
  var draft by remember { mutableStateOf("") }
  Column(modifier = modifier.fillMaxWidth()) {
    Text(label)
    if (values.isNotEmpty()) {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        values.forEach { value ->
          InputChip(
            selected = true,
            onClick = {},
            label = { Text(value) },
            trailingIcon = {
              IconButton(
                modifier = Modifier.size(InputChipDefaults.IconSize),
                onClick = { onRemove(value) },
              ) {
                Icon(
                  painter = painterResource(R.drawable.close),
                  contentDescription = stringResource(R.string.edit_item_remove_chip, value),
                )
              }
            },
          )
        }
      }
    }

    val trimmedDraft = draft.trim()
    val filteredSuggestions =
      remember(suggestions, values, trimmedDraft) {
        (suggestions - values.toSet()).filter {
          trimmedDraft.isBlank() || it.contains(trimmedDraft, ignoreCase = true)
        }
      }
    val showSuggestions =
      suggestions.isNotEmpty() && WindowInsets.isImeVisible && filteredSuggestions.isNotEmpty()

    if (suggestions.isEmpty()) {
      OutlinedTextField(
        value = draft,
        onValueChange = { draft = it },
        placeholder = { Text(stringResource(R.string.edit_item_add_chip, label)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions =
          KeyboardActions(
            onDone = {
              val trimmed = draft.trim()
              if (trimmed.isNotEmpty()) {
                onAdd(trimmed)
                draft = ""
              }
            }
          ),
      )
    } else {
      ExposedDropdownMenuBox(
        expanded = showSuggestions,
        onExpandedChange = {},
        modifier = Modifier.fillMaxWidth(),
      ) {
        OutlinedTextField(
          value = draft,
          onValueChange = { draft = it },
          placeholder = { Text(stringResource(R.string.edit_item_add_chip, label)) },
          modifier =
            Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
          singleLine = true,
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
          keyboardActions =
            KeyboardActions(
              onDone = {
                val trimmed = draft.trim()
                if (trimmed.isNotEmpty()) {
                  onAdd(trimmed)
                  draft = ""
                }
              }
            ),
        )
        ExposedDropdownMenu(expanded = showSuggestions, onDismissRequest = {}) {
          filteredSuggestions.forEach { suggestion ->
            DropdownMenuItem(
              text = { Text(suggestion) },
              onClick = {
                onAdd(suggestion)
                draft = ""
              },
              contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
            )
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun ChipInputPreview() {
  PreviewWrapper {
    ChipInput(
      label = "Authors",
      values = listOf("J. R. R. Tolkien", "Christopher Tolkien"),
      onAdd = {},
      onRemove = {},
    )
  }
}
