package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminMetadataSource
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun LibraryAdminScannerItem(
  source: LibraryAdminMetadataSource,
  priority: Int?,
  sourceDescription: String,
  onCheckedChange: (Boolean) -> Unit,
  reorderHandle: @Composable () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().semantics { contentDescription = sourceDescription },
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(priority?.toString().orEmpty(), modifier = Modifier.width(28.dp))
    Text(
      text = source.name,
      modifier = Modifier.weight(1f).padding(end = 16.dp),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Switch(
      modifier = Modifier.semantics { contentDescription = source.name },
      checked = source.enabled,
      onCheckedChange = onCheckedChange,
    )
    reorderHandle()
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdminScannerItemPreview() {
  PreviewWrapper {
    LibraryAdminScannerItem(
      source = LibraryAdminMetadataSource(id = "folderStructure", name = "Folder structure"),
      priority = 1,
      sourceDescription = "Folder structure, priority 1",
      onCheckedChange = {},
      reorderHandle = {},
    )
  }
}
