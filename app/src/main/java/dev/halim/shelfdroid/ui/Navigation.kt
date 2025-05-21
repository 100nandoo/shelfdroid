package dev.halim.shelfdroid.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
  val navController = rememberNavController()

  val startDestination = if (isLoggedIn) Home else Login

  var miniPlayerState by remember { mutableStateOf(MiniPlayerState(false, "")) }

  Column(
    modifier = Modifier.fillMaxSize().statusBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    NavHostContainer(
      navController = navController,
      startDestination = startDestination,
      onShowMiniPlayer = { id -> miniPlayerState = miniPlayerState.copy(isPlaying = true, id = id) },
    )
    MiniPlayerHandler(navController = navController, state = miniPlayerState)
  }
}

@Composable
private fun ColumnScope.NavHostContainer(
  navController: NavHostController,
  startDestination: Any,
  onShowMiniPlayer: (id: String) -> Unit,
) {
  Box(modifier = Modifier.weight(1f)) {
    NavHost(navController = navController, startDestination = startDestination) {
      composable<Login> {
        LoginScreen(
          onLoginSuccess = { navController.navigate(Home) { popUpTo(Login) { inclusive = true } } }
        )
      }
      composable<Home> {
        HomeScreen(
          onSettingsClicked = { navController.navigate(Settings) },
          onPodcastClicked = { id -> navController.navigate(Podcast(id)) },
          onBookClicked = { id -> navController.navigate(Book(id)) },
        )
      }
      composable<Podcast> { PodcastScreen() }
      composable<Book> { BookScreen(onPlayClicked = { id -> onShowMiniPlayer(id) }) }

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
