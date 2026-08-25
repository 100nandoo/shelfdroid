package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdministrationItem(library: LibraryAdministrationLibrary) {
  ListItem(
    headlineContent = { Text(library.name) },
    supportingContent = {
      Column {
        Text(libraryTypeText(library.mediaType))
        Text(stringResource(R.string.library_identity, library.id))
      }
    },
  )
}

@Composable
private fun libraryTypeText(mediaType: LibraryAdministrationMediaType): String =
  when (mediaType) {
    LibraryAdministrationMediaType.BOOK -> stringResource(R.string.book_library)
    LibraryAdministrationMediaType.PODCAST -> stringResource(R.string.podcast_library)
    LibraryAdministrationMediaType.UNKNOWN -> stringResource(R.string.library_type_unknown)
  }

@ShelfDroidPreview
@Composable
private fun LibraryAdministrationItemPreview() {
  PreviewWrapper {
    LibraryAdministrationItem(
      library =
        LibraryAdministrationLibrary(
          id = "books",
          name = "Books",
          mediaType = LibraryAdministrationMediaType.BOOK,
          displayOrder = 0,
        )
    )
  }
}
