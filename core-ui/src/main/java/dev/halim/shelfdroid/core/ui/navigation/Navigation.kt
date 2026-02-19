package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.NavResultKey
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySnackbarHost
import dev.halim.shelfdroid.core.ui.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.screen.addepisode.AddEpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.addpodcast.AddPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.episode.EpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.listeningsession.ListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.opensession.OpenSessionScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.searchpodcast.SearchPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settings.listeningsession.SettingsListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.settings.podcast.SettingsPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settingsplayback.SettingsPlaybackScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.UserSettingsScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.edit.UserSettingsEditUserScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable data class Login(val reLogin: Boolean = false)

@Serializable data class Home(val fromLogin: Boolean)

@Serializable object Settings

@Serializable object SettingsPlayback

@Serializable object SettingsPodcast

@Serializable object SettingsListeningSession

@Serializable data class SearchPodcast(val libraryId: String)

@Serializable data class Podcast(val id: String)

@Serializable data class Book(val id: String)

@Serializable data class Episode(val itemId: String, val episodeId: String)

@Serializable data class AddEpisode(val id: String)

@Serializable object ListeningSession

@Serializable object OpenSession

@Serializable object UsersSettings

@Serializable object Libraries

@Serializable object ApiKeys

@Serializable object ServerSettings

@Serializable object Logs

@Serializable object Backups

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
  Box(Modifier.weight(1f)) {
    MySnackbarHost(snackbarHostState = snackbarHostState)

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
            onSessionClicked = { navController.navigate(ListeningSession) },
            onOpenSessionClicked = { navController.navigate(OpenSession) },
            onUsersClicked = { navController.navigate(UsersSettings) },
            onLibrariesClicked = {
              //              navController.navigate(Libraries)
            },
            onApiKeysClicked = {
              // navController.navigate(ApiKeys)
            },
            onServerSettingsClicked = {
              //              navController.navigate(ServerSettings)
            },
            onLogsClicked = {
              //              navController.navigate(Logs)
            },
            onBackupsClicked = {
              //              navController.navigate(Backups)
            },
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
        val message = stringResource(R.string.starting_to_download_episodes)

        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          AddEpisodeScreen(
            snackbarHostState = snackbarHostState,
            onDownloadEpisodeSuccess = {
              navController.popBackStack()
              scope.launch { snackbarHostState.showSnackbar(message) }
            },
          )
        }
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
          onPodcastClicked = { navController.navigate(SettingsPodcast) },
          onListeningSessionClicked = { navController.navigate(SettingsListeningSession) },
          reLogin = { navController.navigate(Login(reLogin = true)) },
        )
      }

      composable<SettingsPlayback> { SettingsPlaybackScreen() }
      composable<SettingsPodcast> { SettingsPodcastScreen() }
      composable<SettingsListeningSession> { SettingsListeningSessionScreen() }

      composable<SearchPodcast> { entry ->
        val result = entry.savedStateHandle.get<CreatePodcastNavResult>(NavResultKey.CREATE_PODCAST)
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

        AddPodcastScreen(
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
      composable<ListeningSession> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          ListeningSessionScreen(snackbarHostState = snackbarHostState)
        }
      }

      composable<OpenSession> {
        SharedScreenWrapper(sharedTransitionScope, this@composable) {
          OpenSessionScreen(snackbarHostState = snackbarHostState)
        }
      }

      composable<UsersSettings> { entry ->
        val result = entry.savedStateHandle.get<String>(NavResultKey.UPDATE_USER)

        UserSettingsScreen(
          snackbarHostState = snackbarHostState,
          onUserClicked = { navController.navigate(it) },
          result = result,
        )
        if (result != null) {
          entry.savedStateHandle.remove<String>(NavResultKey.UPDATE_USER)
        }
      }

      composable<NavUsersSettingsEditUser> {
        UserSettingsEditUserScreen(
          snackbarHostState = snackbarHostState,
          onUpdateSuccess = { userId ->
            navController.previousBackStackEntry
              ?.savedStateHandle
              ?.set(NavResultKey.UPDATE_USER, userId)
            navController.popBackStack()
          },
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
