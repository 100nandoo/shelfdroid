package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdministrationItem(
  library: LibraryAdministrationLibrary,
  canMoveUp: Boolean = false,
  canMoveDown: Boolean = false,
  onMoveUp: () -> Unit = {},
  onMoveDown: () -> Unit = {},
  onDragMove: (delta: Int) -> Unit = {},
  reorderEnabled: Boolean = false,
) {
  var dragDistance = 0f
  val moveUpDescription = stringResource(R.string.move_library_up)
  val moveDownDescription = stringResource(R.string.move_library_down)
  val dragModifier =
    if (reorderEnabled) {
      Modifier.pointerInput(library.id) {
        detectDragGesturesAfterLongPress(
          onDragStart = { dragDistance = 0f },
          onDragCancel = { dragDistance = 0f },
          onDragEnd = { dragDistance = 0f },
        ) { change, dragAmount ->
          change.consume()
          dragDistance += dragAmount.y
          // A row-height threshold makes drag reorder deterministic and keeps small pointer
          // movement from generating a stream of server requests.
          if (dragDistance <= -48f) {
            onDragMove(-1)
            dragDistance = 0f
          } else if (dragDistance >= 48f) {
            onDragMove(1)
            dragDistance = 0f
          }
        }
      }
    } else Modifier

  ListItem(
    modifier = dragModifier.fillMaxWidth(),
    headlineContent = { Text(library.name) },
    supportingContent = {
      Column {
        Text(libraryTypeText(library.mediaType))
        Text(stringResource(R.string.library_identity, library.id))
      }
    },
    trailingContent = {
      Row(modifier = Modifier.padding(start = 8.dp)) {
        TextButton(
          enabled = reorderEnabled && canMoveUp,
          onClick = onMoveUp,
          modifier =
            Modifier.semantics {
              contentDescription = moveUpDescription
            },
        ) {
          Text(stringResource(R.string.move_up))
        }
        TextButton(
          enabled = reorderEnabled && canMoveDown,
          onClick = onMoveDown,
          modifier =
            Modifier.semantics {
              contentDescription = moveDownDescription
            },
        ) {
          Text(stringResource(R.string.move_down))
        }
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
