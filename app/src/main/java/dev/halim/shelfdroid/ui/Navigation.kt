@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.ui

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.components.MiniPlayer
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.player.PlayerScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.version
import kotlinx.coroutines.launch
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

    var currentId by remember { mutableStateOf("") }

    val scaffoldState =
      rememberBottomSheetScaffoldState(
        bottomSheetState =
          rememberStandardBottomSheetState(
            initialValue = SheetValue.Hidden,
            skipHiddenState = false,
          )
      )

    val scope = rememberCoroutineScope()

    fun updateMiniPlayerVisibility(route: String?, isShow: Boolean) {
      val isHomeScreen = route?.contains("Home") == true
      val isBookScreen = route?.contains("Book") == true
      val isPodcastScreen = route?.contains("Podcast") == true
      val showOnCurrentScreen = isHomeScreen || isBookScreen || isPodcastScreen
      if (showOnCurrentScreen && isShow) {
        scope.launch { scaffoldState.bottomSheetState.show() }
      } else {
        scope.launch { scaffoldState.bottomSheetState.hide() }
      }
    }

    LaunchedEffect(navController) {
      navController.addOnDestinationChangedListener { _, destination, _ ->
        updateMiniPlayerVisibility(destination.route, scaffoldState.bottomSheetState.isVisible)
      }
    }

    MiniPlayer(
      scaffoldState,
      currentId,
      { navController.navigate(Player(currentId)) },
      { paddingValues ->
        NavHostContainer(
          paddingValues = paddingValues,
          navController = navController,
          startDestination = startDestination,
          this@SharedTransitionLayout,
          onShowMiniPlayer = { id ->
            val currentBackStackEntry = navController.currentBackStackEntry
            val currentDestination = currentBackStackEntry?.destination?.route
            updateMiniPlayerVisibility(currentDestination, true)
            currentId = id
          },
        )
      },
    )
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
