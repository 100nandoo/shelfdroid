@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
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
      val isVisibleRoute =
        route?.contains("Home") == true ||
          route?.contains("Book") == true ||
          route?.contains("Podcast") == true ||
          route?.contains("Episode") == true

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
