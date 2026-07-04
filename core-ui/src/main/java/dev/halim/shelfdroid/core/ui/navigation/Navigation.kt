package dev.halim.shelfdroid.core.ui.navigation

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.runtime.result.LocalResultEventBus
import androidx.navigation3.runtime.result.rememberResultEventBusNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import dev.halim.shelfdroid.core.navigation.ApiKeyChangedNavResult
import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySnackbarHost
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.screen.addepisode.AddEpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.addpodcast.AddPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.apikeys.ApiKeysScreen
import dev.halim.shelfdroid.core.ui.screen.apikeys.createedit.CreateEditApiKeysScreen
import dev.halim.shelfdroid.core.ui.screen.backups.BackupsScreen
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.editepisode.EditEpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemScreen
import dev.halim.shelfdroid.core.ui.screen.episode.EpisodeScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.listeningsession.ListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.logs.LogsScreen
import dev.halim.shelfdroid.core.ui.screen.opensession.OpenSessionScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.searchpodcast.SearchPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.serversettings.ServerSettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settings.listeningsession.SettingsListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.settings.notification.SettingsNotificationScreen
import dev.halim.shelfdroid.core.ui.screen.settings.player.SettingsPlayerScreen
import dev.halim.shelfdroid.core.ui.screen.settings.podcast.SettingsPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settingsplayback.SettingsPlaybackScreen
import dev.halim.shelfdroid.core.ui.screen.userinfo.UserInfoScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.UserSettingsScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.changepassword.ChangePasswordScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.edit.EditUserScreen
import dev.halim.shelfdroid.media.service.PlayerStore
import kotlinx.coroutines.launch

@Composable
fun MainNavigation(
  isLoggedIn: Boolean,
  loginKey: Login,
  playerStore: PlayerStore,
  playerController: PlayerController,
  navRequest: NavRequest,
  onNavRequestComplete: () -> Unit = {},
) {
  SharedTransitionLayout {
    val backStack = rememberShelfNavBackStack(if (isLoggedIn) Home(false) else loginKey)
    val navigator = rememberShelfNavigator(backStack)

    LaunchedEffect(isLoggedIn, loginKey) {
      enforceAuthRestorePolicy(navigator, isLoggedIn, loginKey)
    }
    LaunchedEffect(navRequest, isLoggedIn) {
      handleNavRequest(
        navRequest = navRequest,
        isLoggedIn = isLoggedIn,
        navigator = navigator,
        onNavRequestComplete = onNavRequestComplete,
        playerController = playerController,
      )
    }

    Column {
      NavHostContainer(
        navigator = navigator,
        backStack = backStack,
        sharedTransitionScope = this@SharedTransitionLayout,
        playerStore = playerStore,
        playerController = playerController,
      )
      PlayerHandler(
        currentKey = navigator.current,
        sharedTransitionScope = this@SharedTransitionLayout,
        playerStore = playerStore,
        playerController = playerController,
      )
    }
  }
}

@Composable
private fun ColumnScope.NavHostContainer(
  navigator: ShelfNavigator,
  backStack: androidx.navigation3.runtime.NavBackStack<ShelfNavKey>,
  sharedTransitionScope: SharedTransitionScope,
  playerStore: PlayerStore,
  playerController: PlayerController,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val entryProvider =
    entryProvider<ShelfNavKey> {
      entry<Login> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          LoginScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            onLoginSuccess = { navigator.replaceStack(Home(fromLogin = true)) },
          )
        }
      }
      entry<Home> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          HomeScreen(
            navKey = key,
            onSettingsClicked = { navigator.navigate(Settings) },
            onPodcastClicked = { navigator.navigate(Podcast(it)) },
            onBookClicked = { navigator.navigate(Book(it)) },
            onSearchClicked = { navigator.navigate(SearchPodcast(it)) },
            onSessionClicked = { navigator.navigate(ListeningSession) },
            onOpenSessionClicked = { navigator.navigate(OpenSession) },
            onUsersClicked = { navigator.navigate(UsersSettings) },
            onLibrariesClicked = {},
            onApiKeysClicked = { navigator.navigate(NavApiKeys) },
            onServerSettingsClicked = { navigator.navigate(ServerSettings) },
            onLogsClicked = { navigator.navigate(Logs) },
            onBackupsClicked = { navigator.navigate(Backups) },
            onEditItemClicked = { navigator.navigate(EditItem(it)) },
          )
        }
      }
      entry<Podcast> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          PodcastScreen(
            navKey = key,
            playerController = playerController,
            snackbarHostState = snackbarHostState,
            onEpisodeClicked = { itemId, episodeId ->
              navigator.navigate(Episode(itemId = itemId, episodeId = episodeId))
            },
            onEditEpisodeClicked = { itemId, episodeId ->
              navigator.navigate(EditEpisode(itemId = itemId, episodeId = episodeId))
            },
            onFetchEpisodeSuccess = { itemId -> navigator.navigate(AddEpisode(itemId)) },
          )
        }
      }
      entry<SearchPodcast> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          SearchPodcastScreen(
            navKey = key,
            collectNavResultEvent = true,
            onItemClicked = { payload -> navigator.navigate(AddPodcast(payload)) },
            onAddedClick = { id -> navigator.navigate(Podcast(id)) },
          )
        }
      }
      entry<AddPodcast> { key ->
        val message = stringResource(R.string.podcast_created_successfully)
        val resultBus = LocalResultEventBus.current

        Nav3ScreenWrapper(sharedTransitionScope) {
          AddPodcastScreen(
            navKey = key,
            onCreateSuccess = { result ->
              scope.launch { snackbarHostState.showSuccessSnackbar(message) }
              resultBus.sendResult(result = result)
              completeCreatePodcastNavigation(navigator, result)
            },
          )
        }
      }
      entry<AddEpisode> { key ->
        val message = stringResource(R.string.starting_to_download_episodes)

        Nav3ScreenWrapper(sharedTransitionScope) {
          AddEpisodeScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            onDownloadEpisodeSuccess = {
              navigator.pop()
              scope.launch { snackbarHostState.showSuccessSnackbar(message) }
            },
          )
        }
      }
      entry<Book> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          BookScreen(
            navKey = key,
            playerStore = playerStore,
            playerController = playerController,
            snackbarHostState = snackbarHostState,
            onEditClicked = { navigator.navigate(EditItem(it)) },
          )
        }
      }
      entry<EditItem> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          EditItemScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            navigateBack = { navigator.pop() },
          )
        }
      }
      entry<Episode> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          EpisodeScreen(
            navKey = key,
            playerStore = playerStore,
            playerController = playerController,
            snackbarHostState = snackbarHostState,
            onEditClicked = { navigator.navigate(it) },
          )
        }
      }
      entry<EditEpisode> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          EditEpisodeScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            navigateBack = { navigator.pop() },
          )
        }
      }
      entry<Settings> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          SettingsScreen(
            onPlayerClicked = { navigator.navigate(SettingsPlayer) },
            onPlaybackClicked = { navigator.navigate(SettingsPlayback) },
            onNotificationClicked = { navigator.navigate(SettingsNotification) },
            onPodcastClicked = { navigator.navigate(SettingsPodcast) },
            onListeningSessionClicked = { navigator.navigate(SettingsListeningSession) },
            changePassword = { navigator.navigate(ChangePassword) },
          )
        }
      }
      entry<SettingsPlayback> {
        Nav3ScreenWrapper(sharedTransitionScope) { SettingsPlaybackScreen() }
      }
      entry<SettingsPlayer> { Nav3ScreenWrapper(sharedTransitionScope) { SettingsPlayerScreen() } }
      entry<SettingsNotification> {
        Nav3ScreenWrapper(sharedTransitionScope) { SettingsNotificationScreen() }
      }
      entry<SettingsPodcast> {
        Nav3ScreenWrapper(sharedTransitionScope) { SettingsPodcastScreen() }
      }
      entry<SettingsListeningSession> {
        Nav3ScreenWrapper(sharedTransitionScope) { SettingsListeningSessionScreen() }
      }
      entry<ListeningSession> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          ListeningSessionScreen(snackbarHostState = snackbarHostState)
        }
      }
      entry<OpenSession> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          OpenSessionScreen(snackbarHostState = snackbarHostState)
        }
      }
      entry<UsersSettings> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          UserSettingsScreen(
            snackbarHostState = snackbarHostState,
            onUserClicked = { navigator.navigate(EditUser(it)) },
            onInfoClicked = { navigator.navigate(UserInfo(it)) },
            createUserClicked = { navigator.navigate(EditUser(NavEditUser.defaultUser())) },
          )
        }
      }
      entry<EditUser> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) {
          EditUserScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            navigateBack = { navigator.pop() },
            changePassword = { navigator.navigate(ChangePassword) },
          )
        }
      }
      entry<UserInfo> { key ->
        Nav3ScreenWrapper(sharedTransitionScope) { UserInfoScreen(navKey = key) }
      }
      entry<ChangePassword> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          ChangePasswordScreen(
            snackbarHostState = snackbarHostState,
            finish = { navigator.pop() },
          )
        }
      }
      entry<NavApiKeys> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          ApiKeysScreen(
            collectNavResultEvent = true,
            snackbarHostState = snackbarHostState,
            onEditClicked = { navigator.navigate(EditApiKeys(it)) },
            createApiKeyClicked = { navigator.navigate(EditApiKeys(NavEditApiKeys())) },
          )
        }
      }
      entry<EditApiKeys> { key ->
        val resultBus = LocalResultEventBus.current
        Nav3ScreenWrapper(sharedTransitionScope) {
          CreateEditApiKeysScreen(
            navKey = key,
            snackbarHostState = snackbarHostState,
            navigateBack = {
              resultBus.sendResult(result = ApiKeyChangedNavResult)
              completeApiKeyEditNavigation(navigator)
            },
          )
        }
      }
      entry<ServerSettings> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          ServerSettingsScreen(snackbarHostState = snackbarHostState)
        }
      }
      entry<Logs> { Nav3ScreenWrapper(sharedTransitionScope) { LogsScreen() } }
      entry<Backups> {
        Nav3ScreenWrapper(sharedTransitionScope) {
          BackupsScreen(snackbarHostState = snackbarHostState)
        }
      }
    }

  Box(Modifier.weight(1f)) {
    MySnackbarHost(snackbarHostState = snackbarHostState)
    NavDisplay(
      backStack = backStack,
      onBack = { navigator.pop() },
      entryProvider = entryProvider,
      entryDecorators =
        listOf(
          rememberSaveableStateHolderNavEntryDecorator(),
          rememberViewModelStoreNavEntryDecorator(),
          rememberResultEventBusNavEntryDecorator(),
        ),
      sharedTransitionScope = sharedTransitionScope,
      transitionSpec = {
        fadeIn() togetherWith fadeOut()
      },
      popTransitionSpec = {
        fadeIn() togetherWith fadeOut()
      },
      predictivePopTransitionSpec = {
        fadeIn() togetherWith fadeOut()
      },
    )
  }
}

@Composable
private fun Nav3ScreenWrapper(
  sharedTransitionScope: SharedTransitionScope,
  content: @Composable () -> Unit,
) {
  val animatedContentScope = LocalNavAnimatedContentScope.current
  CompositionLocalProvider(
    LocalSharedTransitionScope provides sharedTransitionScope,
    LocalAnimatedContentScope provides animatedContentScope,
  ) {
    content()
  }
}
