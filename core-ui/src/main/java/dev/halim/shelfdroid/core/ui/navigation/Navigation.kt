package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.PlayerState
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.NavResultKey
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.screen.addepisode.AddEpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.episode.EpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.podcastfeed.PodcastFeedScreen
import dev.halim.shelfdroid.core.ui.screen.searchpodcast.SearchPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settingsplayback.SettingsPlaybackScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data class Login(val reLogin: Boolean = false)

@Serializable data class Home(val fromLogin: Boolean)

@Serializable object Settings

@Serializable object SettingsPlayback

@Serializable data class SearchPodcast(val libraryId: String)

@Serializable data class Podcast(val id: String)

@Serializable data class Book(val id: String)

@Serializable data class Episode(val itemId: String, val episodeId: String)

@Serializable data class AddEpisode(val id: String)

@Composable
fun MainNavigation(
  isLoggedIn: Boolean,
  viewModel: PlayerViewModel = hiltViewModel(),
  navRequest: NavRequest,
  onNavRequestComplete: () -> Unit = {},
) {
  SharedTransitionLayout {
    val navController = rememberNavController()
    val startDestination = if (isLoggedIn) Home(false) else Login()

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
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  Scaffold(modifier = Modifier.weight(1f)) { paddingValues ->
    val playerUiState = playerViewModel.uiState.collectAsStateWithLifecycle()

    val bottom =
      if (playerUiState.value.state == PlayerState.Small) 0.dp
      else paddingValues.calculateBottomPadding()

    Box(
      modifier =
        Modifier.padding(
          start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
          top = paddingValues.calculateTopPadding(),
          end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
          bottom = bottom,
        )
    ) {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.TopCenter).imePadding().zIndex(1f),
      )

      NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
          LoginScreen(
            snackbarHostState = snackbarHostState,
            onLoginSuccess = {
              navController.navigate(Home(true)) {
                launchSingleTop = true
                popUpTo(0) { inclusive = true }
              }
            },
          )
        }
        composable<Home> {
          SharedScreenWrapper(sharedTransitionScope, this@composable) {
            HomeScreen(
              onSettingsClicked = { navController.navigate(Settings) },
              onPodcastClicked = { id -> navController.navigate(Podcast(id)) },
              onBookClicked = { id -> navController.navigate(Book(id)) },
              onSearchClicked = { libraryId -> navController.navigate(SearchPodcast(libraryId)) },
            )
          }
        }
        composable<Podcast> {
          SharedScreenWrapper(sharedTransitionScope, this@composable) {
            PodcastScreen(
              playerViewModel = playerViewModel,
              snackbarHostState = snackbarHostState,
              onEpisodeClicked = { itemId, episodeId ->
                navController.navigate(Episode(itemId, episodeId))
              },
              onFetchEpisodeSuccess = { id -> navController.navigate(AddEpisode(id)) },
            )
          }
        }
        composable<AddEpisode> {
          SharedScreenWrapper(sharedTransitionScope, this@composable) { AddEpisodeScreen() }
        }
        composable<Book> {
          SharedScreenWrapper(sharedTransitionScope, this@composable) {
            BookScreen(playerViewModel = playerViewModel, snackbarHostState = snackbarHostState)
          }
        }

        composable<Episode> {
          SharedScreenWrapper(sharedTransitionScope, this@composable) {
            EpisodeScreen(playerViewModel = playerViewModel, snackbarHostState = snackbarHostState)
          }
        }

        composable<Settings> {
          SettingsScreen(
            onPlaybackClicked = { navController.navigate(SettingsPlayback) },
            reLogin = { navController.navigate(Login(reLogin = true)) },
          )
        }

        composable<SettingsPlayback> { SettingsPlaybackScreen() }

        composable<SearchPodcast> { entry ->
          val result =
            entry.savedStateHandle.get<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST)
          SearchPodcastScreen(
            result = result,
            onItemClicked = { payload -> navController.navigate(payload) },
            onAddedClick = { id -> navController.navigate(Podcast(id)) },
          )
          if (result != null) {
            entry.savedStateHandle.remove<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST)
          }
        }
        composable<PodcastFeedNavPayload> {
          val message = stringResource(R.string.podcast_created_successfully)

          PodcastFeedScreen(
            onCreateSuccess = { result ->
              scope.launch { snackbarHostState.showSnackbar(message) }

              navController.previousBackStackEntry
                ?.savedStateHandle
                ?.set(NavResultKey.CREATE_PODCAST, result)
              navController.popBackStack()
              navController.navigate(Podcast(result.id))
            }
          )
        }
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
