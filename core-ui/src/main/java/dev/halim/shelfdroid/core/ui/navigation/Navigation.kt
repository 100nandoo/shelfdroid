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
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySnackbarHost
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerHandler
import dev.halim.shelfdroid.core.ui.screen.apikeys.ApiKeysScreen
import dev.halim.shelfdroid.core.ui.screen.backups.BackupsScreen
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.listeningsession.ListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.logs.LogsScreen
import dev.halim.shelfdroid.core.ui.screen.opensession.OpenSessionScreen
import dev.halim.shelfdroid.core.ui.screen.serversettings.ServerSettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.core.ui.screen.settings.listeningsession.SettingsListeningSessionScreen
import dev.halim.shelfdroid.core.ui.screen.settings.notification.SettingsNotificationScreen
import dev.halim.shelfdroid.core.ui.screen.settings.player.SettingsPlayerScreen
import dev.halim.shelfdroid.core.ui.screen.settings.podcast.SettingsPodcastScreen
import dev.halim.shelfdroid.core.ui.screen.settingsplayback.SettingsPlaybackScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.UserSettingsScreen
import dev.halim.shelfdroid.core.ui.screen.usersettings.changepassword.ChangePasswordScreen
import dev.halim.shelfdroid.media.service.PlayerStore

@Composable
fun MainNavigation(
  isLoggedIn: Boolean,
  playerStore: PlayerStore,
  playerController: PlayerController,
  navRequest: NavRequest,
  onNavRequestComplete: () -> Unit = {},
) {
  SharedTransitionLayout {
    val backStack = rememberShelfNavBackStack(if (isLoggedIn) Home(false) else Login())
    val navigator = rememberShelfNavigator(backStack)

    LaunchedEffect(isLoggedIn) { enforceAuthRestorePolicy(navigator, isLoggedIn) }
    LaunchedEffect(navRequest.mediaId, navRequest.isOpenPlayer) {
      if (navRequest.mediaId != null) {
        onNavRequestComplete()
      }
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
  val entryProvider = entryProvider<ShelfNavKey> {
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
          onPodcastClicked = {},
          onBookClicked = {},
          onSearchClicked = {},
          onSessionClicked = { navigator.navigate(ListeningSession) },
          onOpenSessionClicked = { navigator.navigate(OpenSession) },
          onUsersClicked = { navigator.navigate(UsersSettings) },
          onLibrariesClicked = {},
          onApiKeysClicked = { navigator.navigate(NavApiKeys) },
          onServerSettingsClicked = { navigator.navigate(ServerSettings) },
          onLogsClicked = { navigator.navigate(Logs) },
          onBackupsClicked = { navigator.navigate(Backups) },
          onEditItemClicked = {},
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
          reLogin = { navigator.navigate(Login(reLogin = true)) },
          changePassword = { navigator.navigate(ChangePassword) },
        )
      }
    }
    entry<SettingsPlayback> { Nav3ScreenWrapper(sharedTransitionScope) { SettingsPlaybackScreen() } }
    entry<SettingsPlayer> { Nav3ScreenWrapper(sharedTransitionScope) { SettingsPlayerScreen() } }
    entry<SettingsNotification> {
      Nav3ScreenWrapper(sharedTransitionScope) { SettingsNotificationScreen() }
    }
    entry<SettingsPodcast> { Nav3ScreenWrapper(sharedTransitionScope) { SettingsPodcastScreen() } }
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
          onUserClicked = {},
          onInfoClicked = {},
          createUserClicked = {},
        )
      }
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
          result = null,
          snackbarHostState = snackbarHostState,
          onEditClicked = {},
          createApiKeyClicked = {},
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
