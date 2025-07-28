@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.player

import ItemCover
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Bookmarks
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.AdvancedControl
import dev.halim.shelfdroid.core.ChapterPosition
import dev.halim.shelfdroid.core.MultipleButtonState
import dev.halim.shelfdroid.core.PlaybackProgress
import dev.halim.shelfdroid.core.PlayerBookmark
import dev.halim.shelfdroid.core.PlayerChapter
import dev.halim.shelfdroid.core.extensions.toSleepTimerText
import dev.halim.shelfdroid.core.extensions.toSpeedText
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.AutoSizeText
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.player.bookmark.BookmarkBottomSheet
import dev.halim.shelfdroid.core.ui.player.bookmark.DeleteBookmarkDialog
import dev.halim.shelfdroid.core.ui.player.bookmark.UpdateBookmarkDialog
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.launch

@Composable
fun BigPlayerContent(
  id: String = "",
  isBook: Boolean = true,
  author: String = Defaults.BOOK_AUTHOR,
  title: String = Defaults.BOOK_TITLE,
  cover: String = "",
  progress: PlaybackProgress = PlaybackProgress(),
  advancedControl: AdvancedControl = AdvancedControl(),
  chapters: List<PlayerChapter> = emptyList(),
  currentChapter: PlayerChapter? = PlayerChapter(),
  bookmarks: List<PlayerBookmark> = emptyList(),
  newBookmarkTime: PlayerBookmark = PlayerBookmark(),
  multipleButtonState: MultipleButtonState = MultipleButtonState(),
  onSwipeUp: () -> Unit = {},
  onSwipeDown: () -> Unit = {},
  onEvent: (PlayerEvent) -> Unit = {},
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Column(
        modifier =
          Modifier.mySharedBound(Animations.Companion.Player.containerKey(id))
            .fillMaxSize()
            .pointerInput(Unit) {
              detectVerticalDragGestures { _, dragAmount ->
                if (dragAmount < 0) {
                  onSwipeUp()
                } else {
                  onSwipeDown()
                }
              }
            }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp, alignment = Alignment.Bottom),
      ) {
        BasicPlayerContent(id, author, title, cover)

        BookmarkAndChapter(isBook, chapters, currentChapter, bookmarks, newBookmarkTime, onEvent)

        PlayerProgress(id, progress, multipleButtonState, onEvent)
        BasicPlayerControl(multipleButtonState, id, currentChapter, onEvent)
        AdvancedPlayerControl(advancedControl, onEvent)

        Spacer(modifier = Modifier.height(16.dp))
      }
    }
  }
}

@Composable
fun BasicPlayerContent(id: String, author: String, title: String, cover: String) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      ItemCover(
        Modifier.fillMaxWidth(),
        cover = cover,
        animationKey = Animations.Companion.Player.coverKey(id),
        shape = RoundedCornerShape(32.dp),
      )

      Spacer(modifier = Modifier.height(16.dp))
      AutoSizeText(
        Modifier.fillMaxWidth().mySharedBound(Animations.Companion.Player.titleKey(title)),
        text = title,
        maxLines = 2,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.titleLarge,
      )

      Text(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.authorKey(author)),
        text = author,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
    }
  }
}

@Composable
fun PlayerProgress(
  id: String = "",
  progress: PlaybackProgress = PlaybackProgress(),
  multipleButtonState: MultipleButtonState,
  onEvent: (PlayerEvent) -> Unit,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  var isDragging by remember { mutableStateOf(false) }
  var target by remember { mutableFloatStateOf(0f) }

  val sliderValue = if (isDragging) target else progress.progress
  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = progress.position,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = progress.duration,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Slider(
        value = sliderValue,
        onValueChange = {
          isDragging = true
          target = it
        },
        modifier =
          Modifier.mySharedBound(Animations.Companion.Player.progressKey(id)).fillMaxWidth(),
        onValueChangeFinished = {
          isDragging = false
          onEvent(PlayerEvent.SeekTo(target))
        },
        enabled = multipleButtonState.seekSliderEnabled,
        track = { sliderState ->
          SliderDefaults.Track(sliderState = sliderState, drawStopIndicator = {})
        },
        colors =
          SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
          ),
      )
    }
  }
}

@Composable
fun BasicPlayerControl(
  multipleButtonState: MultipleButtonState,
  id: String = "",
  currentChapter: PlayerChapter? = PlayerChapter(),
  onEvent: (PlayerEvent) -> Unit,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth(),
      ) {
        MyIconButton(
          icon = Icons.Default.SkipPrevious,
          contentDescription = stringResource(R.string.previous_chapter),
          onClick = { onEvent(PlayerEvent.PreviousChapter) },
          enabled =
            currentChapter?.chapterPosition != ChapterPosition.First && currentChapter != null,
        )
        SeekBackButton({ onEvent(PlayerEvent.SeekBack) }, multipleButtonState, id)
        PlayPauseButton({ onEvent(PlayerEvent.PlayPause) }, multipleButtonState, id, 72)
        SeekForwardButton({ onEvent(PlayerEvent.SeekForward) }, multipleButtonState, id)

        MyIconButton(
          icon = Icons.Default.SkipNext,
          contentDescription = stringResource(R.string.next_chapter),
          onClick = { onEvent(PlayerEvent.NextChapter) },
          enabled =
            currentChapter?.chapterPosition != ChapterPosition.Last && currentChapter != null,
        )
      }
    }
  }
}

@Composable
fun AdvancedPlayerControl(advancedControl: AdvancedControl, onEvent: (PlayerEvent) -> Unit) {
  Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
    SpeedSlider(advancedControl.speed, onEvent)
    SleepTimer(advancedControl.sleepTimerLeft, onEvent)
  }
}

@Composable
fun SpeedSlider(speed: Float, onEvent: (PlayerEvent) -> Unit) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    var speed by remember { mutableFloatStateOf(speed) }

    val speedText = remember(speed) { speed.toSpeedText() }

    Text(text = stringResource(R.string.speed, speedText))
    Spacer(modifier = Modifier.height(4.dp))

    Slider(
      modifier = Modifier.width(150.dp),
      value = speed,
      onValueChange = { speed = it },
      onValueChangeFinished = { onEvent(PlayerEvent.ChangeSpeed(speed)) },
      valueRange = 0.5f..2f,
      steps = 5,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimer(sleepTimeLeft: Duration, onEvent: (PlayerEvent) -> Unit) {
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(stringResource(R.string.sleep_timer))

    Spacer(modifier = Modifier.height(4.dp))

    Box(
      modifier =
        Modifier.clip(CircleShape).clickable { scope.launch { sheetState.show() } }.size(48.dp)
    ) {
      if (sleepTimeLeft.inWholeSeconds > 0) {
        Text(
          sleepTimeLeft.toSleepTimerText(),
          modifier = Modifier.align(Alignment.Center),
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          fontWeight = FontWeight.Bold,
        )
      } else {
        Icon(
          modifier = Modifier.size(36.dp).align(Alignment.Center),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          imageVector = Icons.Default.Timer,
          contentDescription = stringResource(R.string.timer),
        )
      }
    }
    SleepTimerBottomSheet(sheetState, onEvent)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(sheetState: SheetState, onEvent: (PlayerEvent) -> Unit) {
  val scope = rememberCoroutineScope()

  val sleepTimerOptions =
    listOf(
      stringResource(R.string.timer_minute, 60) to 60,
      stringResource(R.string.timer_minute, 45) to 45,
      stringResource(R.string.timer_minute, 30) to 30,
      stringResource(R.string.timer_minute, 15) to 15,
      stringResource(R.string.timer_minute, 10) to 10,
      stringResource(R.string.timer_minute, 5) to 5,
      stringResource(R.string.timer_minute, 1) to 1,
    )
  if (sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      Column {
        sleepTimerOptions.forEach { (label, duration) ->
          TextButton(
            shape = RectangleShape,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
              onEvent(PlayerEvent.SleepTimer(duration.minutes))
              scope.launch { sheetState.hide() }
            },
          ) {
            Text(label)
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkAndChapter(
  isBook: Boolean,
  chapters: List<PlayerChapter>,
  currentChapter: PlayerChapter?,
  bookmarks: List<PlayerBookmark>,
  newBookmarkTime: PlayerBookmark,
  onEvent: (PlayerEvent) -> Unit,
) {
  val chapterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val bookmarkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()

  var selectedBookmark by remember { mutableStateOf(PlayerBookmark()) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showUpdateDialog by remember { mutableStateOf(false) }

  Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
    MyIconButton(
      icon = Icons.Default.Bookmarks,
      contentDescription = stringResource(R.string.bookmarks),
      enabled = isBook,
      onClick = {
        onEvent(PlayerEvent.NewBookmarkTime)
        scope.launch { bookmarkSheetState.show() }
      },
    )

    MyIconButton(
      icon = Icons.AutoMirrored.Filled.List,
      contentDescription = stringResource(R.string.chapters),
      enabled = chapters.isNotEmpty(),
      onClick = { scope.launch { chapterSheetState.show() } },
    )
  }
  ChapterBottomSheet(chapterSheetState, chapters, currentChapter, onEvent)
  BookmarkBottomSheet(
    bookmarkSheetState,
    bookmarks,
    newBookmarkTime,
    onEvent,
    {
      selectedBookmark = it
      showDeleteDialog = true
    },
    {
      selectedBookmark = it
      showUpdateDialog = true
    },
  )

  DeleteBookmarkDialog(
    showDialog = showDeleteDialog,
    onConfirm = {
      onEvent(PlayerEvent.DeleteBookmark(selectedBookmark))
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
  )

  UpdateBookmarkDialog(
    showDialog = showUpdateDialog,
    title = stringResource(R.string.update_bookmark),
    bookmarkTitle = selectedBookmark.title,
    onConfirm = {
      onEvent(PlayerEvent.UpdateBookmark(selectedBookmark, it))
      showUpdateDialog = false
    },
    onDismiss = { showUpdateDialog = false },
  )
}

@ShelfDroidPreview
@Composable
fun BigPlayerContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { BigPlayerContent() }
}

@ShelfDroidPreview
@Composable
fun BigPlayerContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { BigPlayerContent() }
}
