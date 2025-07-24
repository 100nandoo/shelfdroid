@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.player.bookmark

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.player.PlayerBookmark
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun BookmarkBottomSheet(
  sheetState: SheetState,
  bookmarks: List<PlayerBookmark>,
  newBookmarkTime: PlayerBookmark,
  onEvent: (PlayerEvent) -> Unit = {},
  onDeleteBookmark: (PlayerBookmark) -> Unit = {},
  onUpdateBookmark: (PlayerBookmark) -> Unit = {},
) {
  val scope = rememberCoroutineScope()

  if (sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      LazyColumn(reverseLayout = true) {
        val isNotAlreadyAdded = bookmarks.map { it.time }.contains(newBookmarkTime.time).not()
        if (newBookmarkTime.time > 0 && isNotAlreadyAdded) {
          item { NewBookmarkRow(newBookmarkTime = newBookmarkTime, onEvent) }
        }
        itemsIndexed(bookmarks) { index, bookmark ->
          BookmarkRow(
            scope,
            sheetState,
            bookmark,
            onEvent,
            { onDeleteBookmark(bookmark) },
            { onUpdateBookmark(bookmark) },
          )
        }
      }
    }
  }
}

@Composable
fun NewBookmarkRow(newBookmarkTime: PlayerBookmark, onEvent: (PlayerEvent) -> Unit) {
  var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    OutlinedTextField(
      value = textFieldValue,
      onValueChange = { textFieldValue = it },
      label = { Text(stringResource(R.string.bookmark_title)) },
      modifier = Modifier.weight(1f).padding(bottom = 8.dp),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
      singleLine = true,
    )

    Spacer(modifier = Modifier.width(8.dp))

    Text(newBookmarkTime.readableTime, style = MaterialTheme.typography.labelMedium)

    Spacer(modifier = Modifier.width(8.dp))

    FilledTonalIconButton(
      onClick = { onEvent(PlayerEvent.CreateBookmark(newBookmarkTime.time, textFieldValue.text)) }
    ) {
      Icon(
        Icons.AutoMirrored.Filled.Send,
        contentDescription = stringResource(R.string.create_bookmark),
      )
    }
    Spacer(modifier = Modifier.width(8.dp))

    Box(modifier = Modifier.size(40.dp))
  }
}

@Composable
private fun BookmarkRow(
  scope: CoroutineScope,
  sheetState: SheetState,
  bookmark: PlayerBookmark,
  onEvent: (PlayerEvent) -> Unit = {},
  onDeleteBookmark: () -> Unit = {},
  onUpdateBookmark: () -> Unit = {},
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      Modifier.fillMaxWidth()
        .clickable {
          onEvent(PlayerEvent.GoToBookmark(bookmark.time))
          scope.launch { sheetState.hide() }
        }
        .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(
      bookmark.title,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )

    Spacer(modifier = Modifier.width(8.dp))

    Text(bookmark.readableTime, style = MaterialTheme.typography.labelMedium)

    Spacer(modifier = Modifier.width(8.dp))

    FilledTonalIconButton(onClick = { onDeleteBookmark() }) {
      Icon(
        Icons.Default.DeleteOutline,
        contentDescription = stringResource(R.string.delete_bookmark),
      )
    }

    FilledTonalIconButton(onClick = { onUpdateBookmark() }) {
      Icon(Icons.Default.ModeEdit, contentDescription = stringResource(R.string.edit_bookmark))
    }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewBookmarkRow() {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  PreviewWrapper {
    BookmarkRow(
      scope = rememberCoroutineScope(),
      sheetState = sheetState,
      bookmark = Defaults.DEFAULT_PLAYER_BOOKMARK,
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewNewBookmarkRow() {
  PreviewWrapper { NewBookmarkRow(Defaults.DEFAULT_PLAYER_BOOKMARK) {} }
}

@ShelfDroidPreview
@Composable
private fun PreviewBookmarkBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val bookmarkSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = density,
      )

    bookmarkSheetState.isVisible
    BookmarkBottomSheet(
      bookmarkSheetState,
      bookmarks = Defaults.DEFAULT_PLAYER_BOOKMARK_LIST,
      newBookmarkTime = PlayerBookmark(),
    )
    LaunchedEffect(Unit) { bookmarkSheetState.show() }
  }
}
