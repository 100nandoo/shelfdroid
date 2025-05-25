@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.components.player.SmallPlayerState
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.version
import kotlinx.serialization.Serializable

@Serializable object Login

@Serializable object Home

@Serializable object Settings

@Serializable data class Podcast(val id: String)

@Serializable data class Book(val id: String)

@Composable
fun MainNavigation(isLoggedIn: Boolean) {
  SharedTransitionLayout {
    val navController = rememberNavController()

    val startDestination = if (isLoggedIn) Home else Login

    var currentId by remember { mutableStateOf("") }

    val smallPlayerState = remember { mutableStateOf(SmallPlayerState.Hidden) }

    val onShowSmallPlayer: (String) -> Unit = { id ->
      currentId = id
      smallPlayerState.value = SmallPlayerState.PartiallyExpanded
    }
    Column {
      NavHostContainer(
        navController = navController,
        startDestination = startDestination,
        this@SharedTransitionLayout,
        onShowSmallPlayer = onShowSmallPlayer,
      )
      PlayerHandler(navController, this@SharedTransitionLayout, smallPlayerState, currentId)
    }
  }
}

@Composable
private fun ColumnScope.NavHostContainer(
  navController: NavHostController,
  startDestination: Any,
  sharedTransitionScope: SharedTransitionScope,
  onShowSmallPlayer: (id: String) -> Unit,
) {
  Box(modifier = Modifier.weight(1f)) {
    NavHost(navController = navController, startDestination = startDestination) {
      composable<Login> {
        LoginScreen(
          onLoginSuccess = { navController.navigate(Home) { popUpTo(Login) { inclusive = true } } }
        )
      }
      composable<Home> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          HomeScreen(
            onSettingsClicked = { navController.navigate(Settings) },
            onPodcastClicked = { id -> navController.navigate(Podcast(id)) },
            onBookClicked = { id -> navController.navigate(Book(id)) },
          )
        }
      }
      composable<Podcast> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) { PodcastScreen() }
      }
      composable<Book> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          BookScreen(onPlayClicked = { id -> onShowSmallPlayer(id) })
        }
      }

      composable<Settings> {
        SettingsScreen(
          version = version,
          onLogoutSuccess = { navController.navigate(Login) { popUpTo(0) { inclusive = true } } },
        )
      }
    }
  }
}

@Composable
private fun SharedScreenWrapper(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalSharedTransitionScope provides sharedTransitionScope,
    LocalAnimatedContentScope provides animatedContentScope,
  ) {
    content()
  }
}
