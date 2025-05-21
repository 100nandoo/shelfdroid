package dev.halim.shelfdroid.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.halim.shelfdroid.core.ui.components.MiniPlayer

data class MiniPlayerState(val isPlaying: Boolean, val id: String)

@Composable
fun MiniPlayerHandler(navController: NavHostController, state: MiniPlayerState) {
  val currentBackStackEntry = navController.currentBackStackEntryAsState()

  val currentDestination = currentBackStackEntry.value?.destination?.route
  val isHomeScreen = currentDestination?.contains("Home") == true
  val isBookScreen = currentDestination?.contains("Book") == true
  val isPodcastScreen = currentDestination?.contains("Podcast") == true

  val showOnCurrentScreen = isHomeScreen || isBookScreen || isPodcastScreen

  val shouldShowMiniPlayer = state.isPlaying && showOnCurrentScreen

  AnimatedVisibility(shouldShowMiniPlayer) {
    MiniPlayer(state.id, { navController.navigate(Player(state.id)) })
  }
}
