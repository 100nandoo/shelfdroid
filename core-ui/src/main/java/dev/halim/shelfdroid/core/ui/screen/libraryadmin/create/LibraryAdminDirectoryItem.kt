package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDirectory
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun LibraryAdminDirectoryItem(directory: LibraryAdminDirectory, onOpen: () -> Unit) {
  ListItem(
    modifier = Modifier.clickable(onClick = onOpen),
    headlineContent = { Text(directory.name) },
    leadingContent = {
      Icon(painterResource(R.drawable.folder_rounded), contentDescription = null)
    },
    trailingContent = {
      Icon(painterResource(R.drawable.chevron_right), contentDescription = null)
    },
  )
}

@ShelfDroidPreview
@Composable
private fun LibraryAdminDirectoryItemPreview() {
  PreviewWrapper {
    LibraryAdminDirectoryItem(LibraryAdminDirectory("/media/audiobooks", "Audiobooks", 0)) {}
  }
}
