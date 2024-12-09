package dev.halim.shelfdroid.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.theme.JetbrainsMonoFontFamily
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import dev.halim.shelfdroid.ui.components.IconButton
import dev.halim.shelfdroid.ui.components.ItemCover
import dev.halim.shelfdroid.ui.components.LibraryItemPlayIcon
import dev.halim.shelfdroid.utility.formatTime
import dev.halim.shelfdroid.utility.sleepTimerMinute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.minutes

@Composable
fun PlayerScreen(paddingValues: PaddingValues, id: String) {
    val viewModel: PlayerViewModel = koinViewModel(parameters = { parametersOf(id) })

    val uiState by viewModel.uiState.collectAsStateWithLifecycle(PlayerUiState())
    val progressUiState by viewModel.playerProgressUiState.collectAsStateWithLifecycle(
        PlayerProgressUiState()
    )
    val advanceUiState by viewModel.advanceUiState.collectAsStateWithLifecycle(AdvanceUiState())
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (uiState.state == PlayerState.Success) {
        val bookPlayerUiState by remember(uiState) { derivedStateOf { uiState.bookPlayerUiState } }
        PlayerScreenContent(
            paddingValues = paddingValues,
            bookPlayerUiState,
            progressUiState,
            playerState,
            bookPlayerUiState.cover,
            bookPlayerUiState.currentChapter.title,
            bookPlayerUiState.author,
            advanceUiState,
            onEvent = { event -> viewModel.onEvent(event) },
        )
    }
}

@Composable
fun PlayerScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    uiState: BookPlayerUiState = BookPlayerUiState(),
    progressUiState: PlayerProgressUiState = PlayerProgressUiState(),
    playerState: MediaPlayerState = MediaPlayerState(),
    imageUrl: String = "",
    title: String = "Chapter 26",
    authorName: String = "Adam",
    advanceUiState: AdvanceUiState = AdvanceUiState(),
    onEvent: (PlayerEvent) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicPlayerContent(imageUrl, title, authorName)
        Spacer(modifier = Modifier.height(16.dp))

        PlayerProgress(progressUiState, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        BasicPlayerControl(uiState.toImpl(), playerState, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        AdvancedPlayerControl(advanceUiState, paddingValues, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        BookmarkAndChapter(uiState, paddingValues, onEvent)
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun BasicPlayerContent(
    url: String,
    title: String,
    authorName: String,
) {
    ItemCover(url, RoundedCornerShape(8.dp))

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = authorName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )

}

@Composable
fun PlayerProgress(
    uiState: PlayerProgressUiState,
    onEvent: (PlayerEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(uiState.currentTime.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatTime(uiState.totalTime.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    Slider(
        value = uiState.progress,
        onValueChange = { onEvent(PlayerEvent.ProgressChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        onValueChangeFinished = { onEvent(PlayerEvent.ProgressChangedFinish(uiState.progress)) },
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
        )
    )
}


@Composable
fun BasicPlayerControl(
    shelfdroidMediaItemImpl: ShelfdroidMediaItemImpl,
    playerState: MediaPlayerState,
    onEvent: (PlayerEvent) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            icon = Icons.Default.SkipPrevious,
            contentDescription = "Previous Chapter",
            onClick = { onEvent(PlayerEvent.PreviousChapter) }
        )

        IconButton(
            icon = Icons.Default.Replay10,
            contentDescription = "Seek Back 10s",
            onClick = { onEvent(PlayerEvent.SeekBack) }
        )

        LibraryItemPlayIcon(
            shelfdroidMediaItemImpl,
            { onEvent(PlayerEvent.PlayBook) },
            playerState
        )

        IconButton(
            icon = Icons.Default.Forward10,
            contentDescription = "Seek Forward 10s",
            onClick = { onEvent(PlayerEvent.SeekForward) }
        )

        IconButton(
            icon = Icons.Default.SkipNext,
            contentDescription = "Next Chapter",
            onClick = { onEvent(PlayerEvent.NextChapter) }
        )

    }
}

@Composable
fun AdvancedPlayerControl(
    uiState: AdvanceUiState,
    paddingValues: PaddingValues,
    onEvent: (PlayerEvent) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        SpeedSlider(uiState.speed, onEvent)

        SleepTimer(uiState, paddingValues, onEvent)
    }
}

@Composable
fun SpeedSlider(speed: Float, onEvent: (PlayerEvent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Speed: ${speed}x")
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            modifier = Modifier
                .semantics { contentDescription = "speed slider" }
                .width(150.dp),
            value = speed,
            onValueChange = { onEvent(PlayerEvent.ChangeSpeed(it)) },
            valueRange = 0.5f..2f,
            steps = 5,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimer(
    uiState: AdvanceUiState,
    paddingValues: PaddingValues,
    onEvent: (PlayerEvent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sleep Timer")

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .clickable { scope.launch { sheetState.show() } }
                .size(48.dp)
        ) {
            if (uiState.sleepTimeLeft > ZERO) {
                Text(
                    sleepTimerMinute(uiState.sleepTimeLeft),
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    imageVector = Icons.Default.Timer,
                    contentDescription = "timer"
                )

            }
        }
        SleepTimerBottomSheet(sheetState, paddingValues, onEvent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(sheetState: SheetState, paddingValues: PaddingValues, onEvent: (PlayerEvent) -> Unit) {
    val scope = rememberCoroutineScope()

    val sleepTimerOptions = listOf(
        "5m" to 5, "10m" to 10, "15m" to 15, "30m" to 30, "45m" to 45, "60m" to 60
    )
    if (sheetState.isVisible) {
        ModalBottomSheet(
            modifier = Modifier.padding(paddingValues),
            sheetState = sheetState, onDismissRequest = { scope.launch { sheetState.hide() } },
        ) {
            Column {
                sleepTimerOptions.forEach { (label, duration) ->
                    TextButton(shape = RectangleShape, modifier = Modifier.fillMaxWidth(), onClick = {
                        onEvent(PlayerEvent.SetSleepTimer(duration.minutes))
                        scope.launch { sheetState.hide() }
                    }) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkAndChapter(uiState: BookPlayerUiState, paddingValues: PaddingValues, onEvent: (PlayerEvent) -> Unit) {
    val chapterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bookmarkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(
            icon = Icons.Default.Bookmark,
            contentDescription = "bookmarks",
            onClick = { scope.launch { bookmarkSheetState.show() } }
        )
        IconButton(
            icon = Icons.AutoMirrored.Filled.List,
            contentDescription = "chapters",
            onClick = { scope.launch { chapterSheetState.show() } }
        )
    }
    ChapterBottomSheet(uiState, chapterSheetState, paddingValues, onEvent)
    BookmarkBottomSheet(uiState, bookmarkSheetState, paddingValues, onEvent)

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterBottomSheet(
    uiState: BookPlayerUiState, sheetState: SheetState, paddingValues: PaddingValues, onEvent:
        (PlayerEvent) -> Unit
) {
    val currentChapter = uiState.currentChapter
    val currentIndex = maxOf(uiState.chapters.indexOf(currentChapter), 0)

    val scope = rememberCoroutineScope()
    val state = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    if (sheetState.isVisible) {
        ModalBottomSheet(
            modifier = Modifier.padding(paddingValues),
            sheetState = sheetState, onDismissRequest = { scope.launch { sheetState.hide() } },
        ) {
            LazyColumn(
                state = state,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(uiState.chapters, key = { _, chapter -> chapter.id }) { index, bookChapter ->
                    ChapterRow(bookChapter, currentChapter, onEvent, index, scope, sheetState)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChapterRow(
    bookChapter: BookChapter,
    currentChapter: BookChapter,
    onEvent: (PlayerEvent) -> Unit,
    index: Int,
    scope: CoroutineScope,
    sheetState: SheetState
) {
    val startTime = formatTime(bookChapter.start.toLong(), true)
    val endTime = formatTime(bookChapter.end.toLong(), true)
    val selected = bookChapter.id == currentChapter.id
    val background =
        if (selected) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.surfaceContainerLow

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .selectable(selected) {
                onEvent(PlayerEvent.JumpToChapter(index))
                scope.launch { sheetState.hide() }
            }
            .padding(horizontal = 16.dp)
    ) {
        Text(
            bookChapter.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$startTime - $endTime",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = JetbrainsMonoFontFamily()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkBottomSheet(
    uiState: BookPlayerUiState, sheetState: SheetState, paddingValues: PaddingValues, onEvent: (PlayerEvent) -> Unit
) {
    val scope = rememberCoroutineScope()
    if (sheetState.isVisible) {
        ModalBottomSheet(
            modifier = Modifier.padding(paddingValues),
            sheetState = sheetState, onDismissRequest = { scope.launch { sheetState.hide() } },
        ) {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(uiState.bookmarks) { index, audioBookmark ->
                    val time = formatTime(audioBookmark.time.toLong())
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable {
                                onEvent(PlayerEvent.JumpToBookmark(index))
                                scope.launch { sheetState.hide() }
                            }
                    ) {
                        Text(
                            audioBookmark.title,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            time,
                            style = MaterialTheme.typography.labelMedium,
                            fontFamily = JetbrainsMonoFontFamily()
                        )
                    }
                }
            }
        }
    }
}
