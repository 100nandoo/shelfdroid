package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.halim.shelfdroid.core.ui.components.MiniPlayer

@Composable
fun MiniPlayerHandler(navController: NavHostController, showMiniPlayer: Boolean) {
  val currentBackStackEntry = navController.currentBackStackEntryAsState()

  val currentDestination = currentBackStackEntry.value?.destination?.route
  val isHomeScreen = currentDestination?.contains("Home") == true
  val isBookScreen = currentDestination?.contains("Book") == true
  val isPodcastScreen = currentDestination?.contains("Podcast") == true

  val showOnCurrentScreen = isHomeScreen || isBookScreen || isPodcastScreen

  val shouldShowMiniPlayer = showMiniPlayer && showOnCurrentScreen

  if (shouldShowMiniPlayer) {
    MiniPlayer()
  }
}
