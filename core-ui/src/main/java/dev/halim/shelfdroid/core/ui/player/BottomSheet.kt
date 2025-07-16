@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.player.PlayerBookmark
import dev.halim.shelfdroid.core.data.screen.player.PlayerChapter
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChapterBottomSheet(
  sheetState: SheetState,
  chapters: List<PlayerChapter>,
  currentChapter: PlayerChapter?,
  onEvent: (PlayerEvent) -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  val state =
    rememberLazyListState(
      initialFirstVisibleItemIndex = chapters.indexOf(currentChapter).coerceAtLeast(0)
    )
  if (sheetState.isVisible && chapters.isNotEmpty()) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      LazyColumn(state = state, reverseLayout = true) {
        itemsIndexed(chapters, key = { _, chapter -> chapter.id }) { index, playerChapter ->
          ChapterRow(index, scope, sheetState, playerChapter, currentChapter, onEvent)
        }
      }
    }
  }
}

@Composable
fun BookmarkBottomSheet(
  sheetState: SheetState,
  bookmarks: List<PlayerBookmark>,
  onEvent: (PlayerEvent) -> Unit,
) {
  val scope = rememberCoroutineScope()

  if (sheetState.isVisible && bookmarks.isNotEmpty()) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      LazyColumn(reverseLayout = true) {
        itemsIndexed(bookmarks) { index, bookmark ->
          BookmarkRow(scope, sheetState, bookmark, onEvent)
        }
      }
    }
  }
}

@Composable
private fun BookmarkRow(
  scope: CoroutineScope,
  sheetState: SheetState,
  bookmark: PlayerBookmark,
  onEvent: (PlayerEvent) -> Unit,
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

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    FilledTonalIconButton(onClick = { showDeleteDialog = true }) {
      Icon(Icons.Default.DeleteOutline, contentDescription = "Delete bookmark")
    }

    FilledTonalIconButton(onClick = { showUpdateDialog = true }) {
      Icon(Icons.Default.ModeEdit, contentDescription = "Edit bookmark")
    }

    DeleteDialog(
      showDialog = showDeleteDialog,
      onConfirm = { onEvent(PlayerEvent.DeleteBookmark(bookmark)) },
      onDismiss = { showDeleteDialog = false },
    )

    var textFieldValue by remember {
      mutableStateOf(TextFieldValue(bookmark.title, TextRange(0, bookmark.title.length)))
    }

    UpdateBookmarkDialog(
      showDialog = showUpdateDialog,
      title = "Update Bookmark",
      textFieldValue = textFieldValue,
      onValueChange = { textFieldValue = it },
      onConfirm = {
        onEvent(PlayerEvent.UpdateBookmark(bookmark, textFieldValue.text))
        showUpdateDialog = false
      },
      onDismiss = { showUpdateDialog = false },
    )
  }
}

@Composable
private fun ChapterRow(
  index: Int,
  scope: CoroutineScope,
  sheetState: SheetState,
  playerChapter: PlayerChapter,
  currentChapter: PlayerChapter?,
  onEvent: (PlayerEvent) -> Unit = {},
) {
  val currentOnEvent by rememberUpdatedState(onEvent)
  val selected =
    remember(playerChapter.id, currentChapter?.id) { playerChapter.id == currentChapter?.id }

  val background =
    if (selected) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surfaceContainerLow

  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      Modifier.fillMaxWidth()
        .background(background)
        .selectable(selected) {
          currentOnEvent(PlayerEvent.ChangeChapter(index))
          scope.launch { sheetState.hide() }
        }
        .padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    Text(
      playerChapter.title,
      style = MaterialTheme.typography.bodyMedium,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      "${playerChapter.startFormattedTime} - ${playerChapter.endFormattedTime}",
      style = MaterialTheme.typography.labelMedium,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewChapterRow() {
  val chapterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  PreviewWrapper {
    ChapterRow(
      index = 0,
      scope = rememberCoroutineScope(),
      sheetState = chapterSheetState,
      playerChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER_LIST[1],
    )
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
private fun PreviewChapterBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val chapterSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = density,
      )

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
    )
    LaunchedEffect(Unit) { chapterSheetState.show() }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewChapterDynamicBottomSheet() {
  PreviewWrapper(true) {
    val density = LocalDensity.current
    val chapterSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = density,
      )

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewManyChaptersBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val chapterSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = density,
      )

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.MANY_PLAYER_CHAPTERS_LIST,
      currentChapter = Defaults.MANY_PLAYER_CHAPTERS_LIST[10],
    )
  }
}
