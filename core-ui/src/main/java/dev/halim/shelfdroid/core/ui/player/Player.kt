package dev.halim.shelfdroid.core.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.player.bigplayer.BigPlayerContent
import dev.halim.shelfdroid.media.service.PlayerStore

@Composable
fun Player(
  sharedTransitionScope: SharedTransitionScope,
  playerStore: PlayerStore,
  playerController: PlayerController,
) {
  val uiState = playerStore.uiState.collectAsStateWithLifecycle()
  AnimatedContent(targetState = uiState.value.state) { targetState ->
    CompositionLocalProvider(
      LocalSharedTransitionScope provides sharedTransitionScope,
      LocalAnimatedContentScope provides this@AnimatedContent,
    ) {
      val onSwipeUp = {
        when (targetState) {
          is PlayerState.Hidden,
          PlayerState.TempHidden,
          PlayerState.Big -> {}
          PlayerState.Small -> playerController.onEvent(PlayerEvent.Big)
        }
      }
      val onSwipeDown = {
        when (targetState) {
          PlayerState.Small -> {
            playerController.onEvent(PlayerEvent.Hidden)
          }
          PlayerState.Big -> playerController.onEvent(PlayerEvent.Small)
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
      Column {
        when (targetState) {
          PlayerState.Small -> {
            SmallPlayerContent(
              id = uiState.value.id,
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.playbackProgress.progress,
              multipleButtonState = uiState.value.multipleButtonState,
              onSeekBackClick = { playerController.onEvent(PlayerEvent.SeekBackButton) },
              onSeekForwardClick = { playerController.onEvent(PlayerEvent.SeekForwardButton) },
              onPlayPauseClick = { playerController.onEvent(PlayerEvent.PlayPauseButton) },
              onClicked = { playerController.onEvent(PlayerEvent.Big) },
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          PlayerState.Big -> {
            BackHandler(enabled = true) { playerController.onEvent(PlayerEvent.Small) }
            BigPlayerContent(
              id = uiState.value.id,
              isBook = uiState.value.episodeId.isBlank(),
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.playbackProgress,
              advancedControl = uiState.value.advancedControl,
              chapters = uiState.value.playerChapters,
              chapterTitleLine = uiState.value.chapterTitleLine,
              bookmarks = uiState.value.playerBookmarks,
              newBookmarkTime = uiState.value.newBookmarkTime,
              currentChapter = uiState.value.currentChapter,
              multipleButtonState = uiState.value.multipleButtonState,
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
              onEvent = { event -> playerController.onEvent(event) },
            )
          }
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
    }
  }
}
