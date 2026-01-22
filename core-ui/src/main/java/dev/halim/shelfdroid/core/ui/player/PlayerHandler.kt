package dev.halim.shelfdroid.core.ui.player

import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import dev.halim.shelfdroid.core.PlayerState

@Composable
fun PlayerHandler(
  navController: NavHostController,
  sharedTransitionScope: SharedTransitionScope,
  viewModel: PlayerViewModel = hiltViewModel(),
) {
  val uiState = viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
      val route = destination.route
      val isVisibleRoute = isPlayerVisibleDestination(route)
      when {
        !isVisibleRoute && uiState.value.state in setOf(PlayerState.Big, PlayerState.Small) ->
          viewModel.onEvent(PlayerEvent.TempHidden)
        isVisibleRoute && uiState.value.state == PlayerState.TempHidden ->
          viewModel.onEvent(PlayerEvent.Small)
      }
    }
  }

  Player(sharedTransitionScope)
}

private val playerVisibleRoutes = listOf("Home", "Book", "Podcast", "Episode")

fun isPlayerVisibleDestination(route: String?): Boolean {
  val routeSanitized = route?.substringAfterLast(".navigation.")
  return playerVisibleRoutes.any { routeSanitized?.startsWith("$it/") == true }
}
