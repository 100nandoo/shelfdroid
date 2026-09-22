package dev.halim.shelfdroid.core.ui.player

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.player.bigplayer.BigPlayerContent
import dev.halim.shelfdroid.media.playback.PlayerStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal const val PlayerCollapseThreshold = 0.5f
internal const val PlayerCollapseFlingVelocity = 1_200f

internal fun shouldCollapsePlayerDrag(progress: Float, velocity: Float): Boolean {
  return progress >= PlayerCollapseThreshold || velocity >= PlayerCollapseFlingVelocity
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Player(
  sharedTransitionScope: SharedTransitionScope,
  playerStore: PlayerStore,
  playerController: PlayerController,
) {
  val uiState = playerStore.uiState.collectAsStateWithLifecycle()
  val playerState = uiState.value.state
  val transitionState = remember { SeekableTransitionState(playerState) }
  val transition = rememberTransition(transitionState, label = "player")
  val compositionScope = rememberCoroutineScope()
  val latestPlayerState by rememberUpdatedState(playerState)
  var dragProgress by remember { mutableFloatStateOf(0f) }
  var isDraggingPlayer by remember { mutableStateOf(false) }

  suspend fun restoreExpandedPlayer() {
    if (latestPlayerState != PlayerState.Big) return
    val fraction = transitionState.fraction
    if (transitionState.targetState == PlayerState.Small && fraction > 0f) {
      val totalDurationMillis =
        (transition.totalDurationNanos / 1_000_000L).toInt().coerceAtLeast(1)
      coroutineScope {
        animate(
          initialValue = fraction,
          targetValue = 0f,
          animationSpec = tween((fraction * totalDurationMillis).toInt().coerceAtLeast(1)),
        ) { value, _ ->
          if (latestPlayerState == PlayerState.Big) {
            this@coroutineScope.launch { transitionState.seekTo(value, PlayerState.Small) }
          }
        }
      }
    }
    if (latestPlayerState == PlayerState.Big) {
      transitionState.snapTo(PlayerState.Big)
    }
  }

  PredictiveBackHandler(enabled = playerState == PlayerState.Big) { backEvent ->
    try {
      backEvent.collect { event ->
        transitionState.seekTo(event.progress, PlayerState.Small)
      }
      if (latestPlayerState == PlayerState.Big) {
        transitionState.animateTo(PlayerState.Small)
        playerController.onEvent(PlayerEvent.Small)
      }
    } catch (exception: CancellationException) {
      if (latestPlayerState == PlayerState.Big) {
        compositionScope.launch { restoreExpandedPlayer() }
      }
      throw exception
    }
  }

  LaunchedEffect(playerState) {
    if (playerState != transitionState.targetState) {
      transitionState.animateTo(playerState)
    }
  }

  LaunchedEffect(isDraggingPlayer, dragProgress) {
    if (isDraggingPlayer) {
      transitionState.seekTo(dragProgress, PlayerState.Small)
    }
  }

  transition.AnimatedContent { targetState ->
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
              playPause = uiState.value.playPause,
              seekControls = uiState.value.seekControls,
              onSeekBackClick = { playerController.onEvent(PlayerEvent.SeekBackButton) },
              onSeekForwardClick = { playerController.onEvent(PlayerEvent.SeekForwardButton) },
              onPlayPauseClick = { playerController.onEvent(PlayerEvent.PlayPauseButton) },
              onClicked = { playerController.onEvent(PlayerEvent.Big) },
              onSwipeUp = onSwipeUp,
              onSwipeDown = onSwipeDown,
            )
          }
          PlayerState.Big -> {
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
              chapterTimeDisplay = uiState.value.chapterTimeDisplay,
              bookmarks = uiState.value.playerBookmarks,
              newBookmarkTime = uiState.value.newBookmarkTime,
              currentChapter = uiState.value.currentChapter,
              playPause = uiState.value.playPause,
              seekControls = uiState.value.seekControls,
              onSwipeUp = onSwipeUp,
              onSwipeDownProgress = { progress ->
                isDraggingPlayer = true
                dragProgress = progress
              },
              onSwipeDownEnd = { progress, velocity ->
                isDraggingPlayer = false
                compositionScope.launch {
                  transitionState.seekTo(progress, PlayerState.Small)
                  if (shouldCollapsePlayerDrag(progress, velocity)) {
                    transitionState.animateTo(PlayerState.Small)
                    playerController.onEvent(PlayerEvent.Small)
                  } else {
                    restoreExpandedPlayer()
                  }
                }
              },
              onSwipeDownCancel = {
                isDraggingPlayer = false
                compositionScope.launch { restoreExpandedPlayer() }
              },
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
