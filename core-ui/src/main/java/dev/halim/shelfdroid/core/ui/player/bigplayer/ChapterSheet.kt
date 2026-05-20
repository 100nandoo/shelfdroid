@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.player.bigplayer

import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ChapterTimeDisplay
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.extensions.formatChapterTime
import dev.halim.shelfdroid.core.extensions.formatDurationShort
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelMedium
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChapterBottomSheet(
  sheetState: SheetState,
  chapters: List<PlayerChapter>,
  currentChapter: PlayerChapter?,
  chapterTitleLine: Int = 2,
  chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.TimeRange,
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
          PlayerChapterRow(
            index,
            scope,
            sheetState,
            playerChapter,
            currentChapter,
            chapterTitleLine,
            chapterTimeDisplay,
            onEvent,
          )
          if (chapterTitleLine != 1) {
            HorizontalDivider()
          }
        }
      }
    }
  }
}

@Composable
fun ChapterRow(
  modifier: Modifier = Modifier,
  title: String,
  chapterTime: String,
  selected: Boolean = false,
  chapterTitleLine: Int = 2,
  onClick: (() -> Unit)? = null,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier =
      modifier
        .fillMaxWidth()
        .then(if (onClick != null) Modifier.selectable(selected, onClick = onClick) else Modifier),
  ) {
    TextBodyMedium(
      modifier = Modifier.weight(1f),
      text = title,
      maxLines = chapterTitleLine,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.width(8.dp))
    TextLabelMedium(text = chapterTime)
  }
}

@Composable
private fun PlayerChapterRow(
  index: Int,
  scope: CoroutineScope,
  sheetState: SheetState,
  playerChapter: PlayerChapter,
  currentChapter: PlayerChapter?,
  chapterTitleLine: Int = 2,
  chapterTimeDisplay: ChapterTimeDisplay = ChapterTimeDisplay.TimeRange,
  onEvent: (PlayerEvent) -> Unit = {},
) {
  val currentOnEvent by rememberUpdatedState(onEvent)
  val selected =
    remember(playerChapter.id, currentChapter?.id) { playerChapter.id == currentChapter?.id }

  val background =
    if (selected) MaterialTheme.colorScheme.surfaceVariant
    else MaterialTheme.colorScheme.surfaceContainerLow
  val chapterTime =
    when (chapterTimeDisplay) {
      ChapterTimeDisplay.TimeRange ->
        "${playerChapter.startFormattedTime} - ${playerChapter.endFormattedTime}"
      ChapterTimeDisplay.Duration ->
        (playerChapter.endTimeSeconds - playerChapter.startTimeSeconds).formatChapterTime()
      ChapterTimeDisplay.DurationShort ->
        (playerChapter.endTimeSeconds - playerChapter.startTimeSeconds).formatDurationShort()
    }
  ChapterRow(
    title = playerChapter.title,
    chapterTime = chapterTime,
    selected = selected,
    chapterTitleLine = chapterTitleLine,
    modifier = Modifier.background(background).padding(horizontal = 16.dp, vertical = 8.dp),
  ) {
    currentOnEvent(PlayerEvent.ChangeChapter(index))
    scope.launch { sheetState.hide() }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewChapterRow() {
  val chapterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  PreviewWrapper {
    PlayerChapterRow(
      index = 0,
      scope = rememberCoroutineScope(),
      sheetState = chapterSheetState,
      playerChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER_LIST.getOrNull(1),
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
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
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
private fun PreviewChapterBottomSheetDuration() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val chapterSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
      )

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
      chapterTimeDisplay = ChapterTimeDisplay.Duration,
    )
    LaunchedEffect(Unit) { chapterSheetState.show() }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewChapterBottomSheetDurationShort() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val chapterSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        positionalThreshold = { with(density) { 56.dp.toPx() } },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
      )

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
      chapterTimeDisplay = ChapterTimeDisplay.DurationShort,
    )
    LaunchedEffect(Unit) { chapterSheetState.show() }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewChapterDynamicBottomSheet() {
  PreviewWrapper(true) {
    val density = LocalDensity.current
    val chapterSheetState = sheetState(density)

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
    val chapterSheetState = sheetState(density)

    chapterSheetState.isVisible
    ChapterBottomSheet(
      chapterSheetState,
      chapters = Defaults.MANY_PLAYER_CHAPTERS_LIST,
      currentChapter = Defaults.MANY_PLAYER_CHAPTERS_LIST.getOrNull(10),
    )
  }
}
