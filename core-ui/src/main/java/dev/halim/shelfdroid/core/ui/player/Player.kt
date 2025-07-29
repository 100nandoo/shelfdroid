@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope

@Composable
fun Player(
  sharedTransitionScope: SharedTransitionScope,
  viewModel: PlayerViewModel = hiltViewModel(),
) {
  val uiState = viewModel.uiState.collectAsState()
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
          PlayerState.Small -> viewModel.onEvent(PlayerEvent.Big)
        }
      }
      val onSwipeDown = {
        when (targetState) {
          PlayerState.Small -> {
            viewModel.onEvent(PlayerEvent.Hidden)
          }
          PlayerState.Big -> viewModel.onEvent(PlayerEvent.Small)
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
              onSeekBackClick = { viewModel.onEvent(PlayerEvent.SeekBackButton) },
              onSeekForwardClick = { viewModel.onEvent(PlayerEvent.SeekForwardButton) },
              onPlayPauseClick = { viewModel.onEvent(PlayerEvent.PlayPauseButton) },
              onClicked = { viewModel.onEvent(PlayerEvent.Big) },
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          PlayerState.Big -> {
            BackHandler(enabled = true) { viewModel.onEvent(PlayerEvent.Small) }
            BigPlayerContent(
              id = uiState.value.id,
              isBook = uiState.value.episodeId.isBlank(),
              author = uiState.value.author,
              title = uiState.value.title,
              cover = uiState.value.cover,
              progress = uiState.value.playbackProgress,
              advancedControl = uiState.value.advancedControl,
              chapters = uiState.value.playerChapters,
              bookmarks = uiState.value.playerBookmarks,
              newBookmarkTime = uiState.value.newBookmarkTime,
              currentChapter = uiState.value.currentChapter,
              multipleButtonState = uiState.value.multipleButtonState,
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
              onEvent = { event -> viewModel.onEvent(event) },
            )
          }
          is PlayerState.Hidden,
          PlayerState.TempHidden -> {}
        }
      }
    }
  }
}
