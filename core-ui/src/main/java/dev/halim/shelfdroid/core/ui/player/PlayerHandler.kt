package dev.halim.shelfdroid.core.ui.player

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.ui.navigation.Book
import dev.halim.shelfdroid.core.ui.navigation.Episode
import dev.halim.shelfdroid.core.ui.navigation.Home
import dev.halim.shelfdroid.core.ui.navigation.Podcast
import dev.halim.shelfdroid.core.ui.navigation.ShelfNavKey
import dev.halim.shelfdroid.media.service.PlayerStore

@Composable
fun PlayerHandler(
  currentKey: ShelfNavKey?,
  sharedTransitionScope: SharedTransitionScope,
  playerStore: PlayerStore,
  playerController: PlayerController,
) {
  val uiState = playerStore.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(currentKey, uiState.value.state) {
    val isVisibleRoute = isPlayerVisibleDestination(currentKey)
    when {
      !isVisibleRoute && uiState.value.state in setOf(PlayerState.Big, PlayerState.Small) ->
        playerController.onEvent(PlayerEvent.TempHidden)
      isVisibleRoute && uiState.value.state == PlayerState.TempHidden ->
        playerController.onEvent(PlayerEvent.Small)
    }
  }

  Player(
    sharedTransitionScope = sharedTransitionScope,
    playerStore = playerStore,
    playerController = playerController,
  )
}

fun isPlayerVisibleDestination(key: ShelfNavKey?): Boolean {
  return when (key) {
    is Home,
    is Book,
    is Podcast,
    is Episode -> true
    else -> false
  }
}
