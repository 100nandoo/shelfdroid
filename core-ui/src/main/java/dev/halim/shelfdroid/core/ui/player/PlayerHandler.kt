package dev.halim.shelfdroid.core.ui.player

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.media.service.PlayerStore

@Composable
fun PlayerHandler(
  navController: NavHostController,
  sharedTransitionScope: SharedTransitionScope,
  playerStore: PlayerStore,
  playerController: PlayerController,
) {
  val uiState = playerStore.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
      val route = destination.route
      val isVisibleRoute = isPlayerVisibleDestination(route)
      when {
        !isVisibleRoute && uiState.value.state in setOf(PlayerState.Big, PlayerState.Small) ->
          playerController.onEvent(PlayerEvent.TempHidden)
        isVisibleRoute && uiState.value.state == PlayerState.TempHidden ->
          playerController.onEvent(PlayerEvent.Small)
      }
    }
  }

  Player(
    sharedTransitionScope = sharedTransitionScope,
    playerStore = playerStore,
    playerController = playerController,
  )
}

private val playerVisibleRoutes = listOf("Home", "Book", "Podcast", "Episode")

fun isPlayerVisibleDestination(route: String?): Boolean {
  val routeSanitized = route?.substringAfterLast(".navigation.")
  return playerVisibleRoutes.any { routeSanitized?.startsWith("$it/") == true }
}
