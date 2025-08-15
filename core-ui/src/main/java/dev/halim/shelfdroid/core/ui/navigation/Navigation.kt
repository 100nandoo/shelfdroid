@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.episode.EpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable object Login

@Serializable object Home

@Serializable object Settings

@Serializable data class Podcast(val id: String)

@Serializable data class Book(val id: String)

@Serializable data class Episode(val itemId: String, val episodeId: String)

@Composable
fun MainNavigation(
  isLoggedIn: Boolean,
  viewModel: PlayerViewModel = hiltViewModel(),
  navRequest: NavRequest,
  onNavRequestComplete: () -> Unit = {},
) {
  SharedTransitionLayout {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Home else Login

    LaunchedEffect(navRequest.mediaId) {
      handlePendingMediaId(navRequest, isLoggedIn, navController, onNavRequestComplete, viewModel)
    }
    Column {
      NavHostContainer(
        navController = navController,
        startDestination = startDestination,
        sharedTransitionScope = this@SharedTransitionLayout,
      )
      PlayerHandler(navController, this@SharedTransitionLayout)
    }
  }
}

@Composable
private fun ColumnScope.NavHostContainer(
  navController: NavHostController,
  startDestination: Any,
  sharedTransitionScope: SharedTransitionScope,
) {
  val playerViewModel: PlayerViewModel = hiltViewModel()

  Scaffold(modifier = Modifier.weight(1f)) { paddingValues ->
    val playerUiState = playerViewModel.uiState.collectAsStateWithLifecycle()

    val bottom =
      if (playerUiState.value.state == PlayerState.Small) 0.dp
      else paddingValues.calculateBottomPadding()

    NavHost(
      modifier =
        Modifier.padding(
          start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
          top = paddingValues.calculateTopPadding(),
          end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
          bottom = bottom,
        ),
      navController = navController,
      startDestination = startDestination,
    ) {
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
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          PodcastScreen(
            playerViewModel = playerViewModel,
            onEpisodeClicked = { itemId, episodeId ->
              navController.navigate(Episode(itemId, episodeId))
            },
          )
        }
      }
      composable<Book> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          BookScreen(playerViewModel = playerViewModel)
        }
      }

      composable<Episode> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          EpisodeScreen(playerViewModel = playerViewModel)
        }
      }

      composable<Settings> { SettingsScreen() }
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
