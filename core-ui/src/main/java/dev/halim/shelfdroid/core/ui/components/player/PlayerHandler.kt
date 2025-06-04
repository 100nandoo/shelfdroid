@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.components.player

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.navigation.NavHostController

enum class SmallPlayerState {
  Hidden,
  TempHidden,
  Expanded,
  PartiallyExpanded,
}

@Composable
fun PlayerHandler(
  navController: NavHostController,
  sharedTransitionScope: SharedTransitionScope,
  smallPlayerState: MutableState<SmallPlayerState>,
  id: String,
) {
  LaunchedEffect(navController) {
    navController.addOnDestinationChangedListener { _, destination, _ ->
      val route = destination.route
      val isVisibleRoute =
        route?.contains("Home") == true ||
          route?.contains("Book") == true ||
          route?.contains("Podcast") == true

      when {
        !isVisibleRoute &&
          smallPlayerState.value in
            setOf(SmallPlayerState.Expanded, SmallPlayerState.PartiallyExpanded) ->
          smallPlayerState.value = SmallPlayerState.TempHidden

        isVisibleRoute && smallPlayerState.value == SmallPlayerState.TempHidden ->
          smallPlayerState.value = SmallPlayerState.PartiallyExpanded
      }
    }
  }

  BigPlayer(sharedTransitionScope, id = id, state = smallPlayerState)
}
