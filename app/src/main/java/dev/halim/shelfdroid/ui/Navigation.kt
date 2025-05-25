@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.components.miniplayer.MiniPlayerHandler
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.player.PlayerScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.version
import kotlinx.serialization.Serializable

@Serializable object Login

@Serializable object Home

@Serializable object Settings

@Serializable data class Podcast(val id: String)

@Serializable data class Book(val id: String)

@Serializable data class Player(val id: String)

@Composable
fun MainNavigation(isLoggedIn: Boolean) {
  SharedTransitionLayout {
    val navController = rememberNavController()

    val startDestination = if (isLoggedIn) Home else Login

    MiniPlayerHandler(navController) { paddingValues, onShowMiniPlayer ->
      NavHostContainer(
        paddingValues = paddingValues,
        navController = navController,
        startDestination = startDestination,
        this@SharedTransitionLayout,
        onShowMiniPlayer = onShowMiniPlayer,
      )
    }
  }
}

@Composable
private fun NavHostContainer(
  paddingValues: PaddingValues,
  navController: NavHostController,
  startDestination: Any,
  sharedTransitionScope: SharedTransitionScope,
  onShowMiniPlayer: (id: String) -> Unit,
) {
  Box(modifier = Modifier.padding(paddingValues)) {
    NavHost(navController = navController, startDestination = startDestination) {
      composable<Login> {
        LoginScreen(
          onLoginSuccess = { navController.navigate(Home) { popUpTo(Login) { inclusive = true } } }
        )
      }
      composable<Home> {
        HomeScreen(
          sharedTransitionScope,
          this@composable,
          onSettingsClicked = { navController.navigate(Settings) },
          onPodcastClicked = { id -> navController.navigate(Podcast(id)) },
          onBookClicked = { id -> navController.navigate(Book(id)) },
        )
      }
      composable<Podcast> {
        PodcastScreen(
          sharedTransitionScope = sharedTransitionScope,
          animatedContentScope = this@composable,
        )
      }
      composable<Book> {
        BookScreen(
          sharedTransitionScope = sharedTransitionScope,
          animatedContentScope = this@composable,
          onPlayClicked = { id -> onShowMiniPlayer(id) },
        )
      }

      composable<Player> { PlayerScreen() }

      composable<Settings> {
        SettingsScreen(
          version = version,
          onLogoutSuccess = { navController.navigate(Login) { popUpTo(0) { inclusive = true } } },
        )
      }
    }
  }
}
