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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.halim.shelfdroid.core.data.screen.player.PlayerChapter
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.AutoSizeText
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.FakePlayer
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun BigPlayerContent(
  player: Player = FakePlayer(),
  id: String = "",
  author: String = Defaults.BOOK_AUTHOR,
  title: String = Defaults.BOOK_TITLE,
  cover: String = "",
  progress: Float = 0f,
  chapters: List<PlayerChapter> = emptyList(),
  currentChapter: PlayerChapter? = PlayerChapter(),
  onSwipeUp: () -> Unit = {},
  onSwipeDown: () -> Unit = {},
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

        BookmarkAndChapter(chapters, currentChapter)

        PlayerProgress(id, progress)

        BasicPlayerControl(player, id)

        AdvancedPlayerControl()

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
fun PlayerProgress(id: String = "", progress: Float = 0f) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
          text = "01:11",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = "03:33",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Slider(
        value = progress,
        onValueChange = {},
        modifier =
          Modifier.mySharedBound(Animations.Companion.Player.progressKey(id)).fillMaxWidth(),
        onValueChangeFinished = {},
        colors =
          SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary,
          ),
      )
    }
  }
}

@UnstableApi
@Composable
fun BasicPlayerControl(
  player: Player = ExoPlayer.Builder(LocalContext.current).build(),
  id: String = "",
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
          contentDescription = "Previous Chapter",
          onClick = {},
        )
        SeekBackButton(player, id)
        PlayPauseButton(player, id, 72)
        SeekForwardButton(player, id)

        MyIconButton(
          icon = Icons.Default.SkipNext,
          contentDescription = "Next Chapter",
          onClick = {},
        )
      }
    }
  }
}

@Composable
fun AdvancedPlayerControl() {
  Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
    SpeedSlider(1.0f)

    SleepTimer()
  }
}

@Composable
fun SpeedSlider(speed: Float) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(text = "Speed: ${speed}x")
    Spacer(modifier = Modifier.height(4.dp))

    Slider(
      modifier = Modifier.semantics { contentDescription = "speed slider" }.width(150.dp),
      value = speed,
      onValueChange = {},
      valueRange = 0.5f..2f,
      steps = 5,
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimer() {
  val sheetState = rememberModalBottomSheetState()
  val scope = rememberCoroutineScope()
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text("Sleep Timer")

    Spacer(modifier = Modifier.height(4.dp))

    Box(
      modifier =
        Modifier.clip(CircleShape).clickable { scope.launch { sheetState.show() } }.size(48.dp)
    ) {
      //            if (uiState.sleepTimeLeft > ZERO) {
      //                Text(
      //                    sleepTimerMinute(uiState.sleepTimeLeft),
      //                    modifier = Modifier.align(Alignment.Center),
      //                    color = MaterialTheme.colorScheme.onSecondaryContainer,
      //                    fontWeight = FontWeight.Bold
      //                )
      //            } else {
      Icon(
        modifier = Modifier.size(36.dp).align(Alignment.Center),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        imageVector = Icons.Default.Timer,
        contentDescription = "timer",
      )

      //            }
    }
    SleepTimerBottomSheet(sheetState)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(sheetState: SheetState) {
  val scope = rememberCoroutineScope()

  val sleepTimerOptions =
    listOf("5m" to 5, "10m" to 10, "15m" to 15, "30m" to 30, "45m" to 45, "60m" to 60)
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
              //                        onEvent(PlayerEvent.SetSleepTimer(duration.minutes))
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
fun BookmarkAndChapter(chapters: List<PlayerChapter>, currentChapter: PlayerChapter?) {
  val chapterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val bookmarkSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val scope = rememberCoroutineScope()
  Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
    MyIconButton(
      icon = Icons.Default.Bookmarks,
      contentDescription = "bookmarks",
      onClick = { scope.launch { bookmarkSheetState.show() } },
    )

    MyIconButton(
      icon = Icons.AutoMirrored.Filled.List,
      contentDescription = "chapters",
      enabled = chapters.isNotEmpty(),
      onClick = { scope.launch { chapterSheetState.show() } },
    )
  }
  ChapterBottomSheet(chapterSheetState, chapters, currentChapter)
  //    BookmarkBottomSheet(bookmarkSheetState)

}

// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun BookmarkBottomSheet(
//    uiState: BookPlayerUiState, sheetState: SheetState, paddingValues: PaddingValues, onEvent:
// (PlayerEvent) -> Unit
// ) {
//    val scope = rememberCoroutineScope()
//    if (sheetState.isVisible) {
//        ModalBottomSheet(
//            modifier = Modifier.padding(paddingValues),
//            sheetState = sheetState, onDismissRequest = { scope.launch { sheetState.hide() } },
//        ) {
//            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
//                itemsIndexed(uiState.bookmarks) { index, audioBookmark ->
//                    val time = formatTime(audioBookmark.time.toLong())
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(horizontal = 16.dp)
//                            .clickable {
//                                onEvent(PlayerEvent.JumpToBookmark(index))
//                                scope.launch { sheetState.hide() }
//                            }
//                    ) {
//                        Text(
//                            audioBookmark.title,
//                            style = MaterialTheme.typography.titleLarge,
//                            maxLines = 1,
//                            overflow = TextOverflow.Ellipsis,
//                            modifier = Modifier.weight(1f)
//                        )
//
//                        Spacer(modifier = Modifier.width(8.dp))
//
//                        Text(
//                            time,
//                            style = MaterialTheme.typography.labelMedium,
//                            fontFamily = JetbrainsMonoFontFamily()
//                        )
//                    }
//                }
//            }
//        }
//    }
// }

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
