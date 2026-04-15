package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun FilesTab(uiState: EditItemUiState) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.edit_item_library_files_count, uiState.libraryFiles.size),
      style = MaterialTheme.typography.titleMedium,
    )
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
        stringResource(R.string.path),
        modifier = Modifier.weight(3f),
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        stringResource(R.string.edit_item_size),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        stringResource(R.string.edit_item_type),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
      )
    }
    HorizontalDivider()
    uiState.libraryFiles.forEach { file ->
      Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(file.path, modifier = Modifier.weight(3f))
        Text(formatSize(file.size), modifier = Modifier.weight(1f))
        Text(file.fileType, modifier = Modifier.weight(1f))
      }
    }
  }
}

private fun formatSize(bytes: Long): String {
  return when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.2f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
  }
}

@ShelfDroidPreview
@Composable
private fun FilesTabPreview() {
  PreviewWrapper { FilesTab(uiState = Defaults.EDIT_ITEM_UI_STATE) }
}
