@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun MatchTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val match = uiState.match
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText =
      match.providers.find { it.value == match.selectedProvider }?.text ?: match.selectedProvider
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
      OutlinedTextField(
        value = selectedText,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.edit_item_provider)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier =
          Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      )
      ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        match.providers.forEach { provider ->
          DropdownMenuItem(
            text = { Text(provider.text) },
            onClick = {
              onEvent(EditItemEvent.UpdateMatchProvider(provider.value))
              expanded = false
            },
          )
        }
      }
    }
    OutlinedTextField(
      value = match.title,
      onValueChange = { onEvent(EditItemEvent.UpdateMatchTitle(it)) },
      label = { Text(stringResource(R.string.edit_item_search_title)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = match.author,
      onValueChange = { onEvent(EditItemEvent.UpdateMatchAuthor(it)) },
      label = { Text(stringResource(R.string.author)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Button(
      onClick = { onEvent(EditItemEvent.RunMatchSearch) },
      enabled = !match.isSearching && match.title.isNotBlank(),
    ) {
      Text(stringResource(R.string.search))
    }

    if (match.isSearching) {
      CircularProgressIndicator()
    }

    match.results.forEachIndexed { index, result ->
      Card(
        modifier =
          Modifier.fillMaxWidth().clickable { onEvent(EditItemEvent.ApplyMatchResult(index)) }
      ) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (result.cover.isNotBlank()) {
            CoverNoAnimation(modifier = Modifier.size(64.dp), coverUrl = result.cover)
          }
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = result.title,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (result.author.isNotBlank()) {
              Text(
                text = stringResource(R.string.edit_item_match_by_author, result.author),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            if (result.description.isNotBlank()) {
              Text(
                text = result.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun MatchTabPreview() {
  PreviewWrapper { MatchTab(uiState = Defaults.EDIT_ITEM_UI_STATE, onEvent = {}) }
}
