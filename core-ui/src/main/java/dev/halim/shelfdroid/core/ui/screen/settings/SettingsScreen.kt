package dev.halim.shelfdroid.core.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.settings.SettingsState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel = hiltViewModel(),
  onPlayerClicked: () -> Unit = {},
  onPlaybackClicked: () -> Unit = {},
  onNotificationClicked: () -> Unit = {},
  onPodcastClicked: () -> Unit = {},
  onListeningSessionClicked: () -> Unit = {},
  changePassword: () -> Unit = {},
  onHomeClicked: () -> Unit = {},
  onLoggedOut: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val version = remember { viewModel.version }
  val logoutFailedMessage = stringResource(R.string.logout_failed_message)
  var logoutErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

  LaunchedEffect(uiState.settingsState) {
    val state = uiState.settingsState
    if (state is SettingsState.Failure) {
      logoutErrorMessage = state.errorMessage ?: logoutFailedMessage
    }
  }

  LaunchedEffect(viewModel) {
    viewModel.events.collect { event ->
      when (event) {
        SettingsUiEvent.LoggedOut -> onLoggedOut()
      }
    }
  }

  SettingsScreenContent(
    uiState = uiState,
    version = version,
    user = uiState.username,
    onPlayerClicked = onPlayerClicked,
    onHomeClicked = onHomeClicked,
    onPlaybackClicked = onPlaybackClicked,
    onNotificationClicked = onNotificationClicked,
    onPodcastClicked = onPodcastClicked,
    onListeningSessionClicked = onListeningSessionClicked,
    changePassword = changePassword,
    onEvent = { settingsEvent -> viewModel.onEvent(settingsEvent) },
  )

  MyAlertDialog(
    showDialog = logoutErrorMessage != null,
    title = stringResource(R.string.logout_failed),
    text = logoutErrorMessage.orEmpty(),
    confirmText = stringResource(R.string.ok),
    dismissText = null,
    onConfirm = { logoutErrorMessage = null },
    onDismiss = {},
  )
}

@Composable
fun SettingsScreenContent(
  uiState: SettingsUiState = SettingsUiState(),
  version: String = Defaults.VERSION,
  user: String = Defaults.USERNAME,
  onPlayerClicked: () -> Unit = {},
  onPlaybackClicked: () -> Unit = {},
  onNotificationClicked: () -> Unit = {},
  onPodcastClicked: () -> Unit = {},
  onListeningSessionClicked: () -> Unit = {},
  changePassword: () -> Unit = {},
  onHomeClicked: () -> Unit = {},
  onEvent: (SettingsEvent) -> Unit = {},
) {
  Column(
    modifier =
      Modifier.fillMaxSize().padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    LogoutSection(onEvent, changePassword)
    Spacer(modifier = Modifier.height(16.dp))

    OthersSection(version, user, uiState)
    Spacer(modifier = Modifier.height(16.dp))

    DisplaySection(uiState, onEvent)
    Spacer(modifier = Modifier.height(16.dp))

    SettingsClickLabel(
      text = stringResource(R.string.home_screen),
      supportingText =
        stringResource(R.string.settings_and_behaviour, stringResource(R.string.home_screen)),
      onClick = onHomeClicked,
    )

    SettingsClickLabel(
      text = stringResource(R.string.player),
      supportingText =
        stringResource(R.string.settings_and_behaviour, stringResource(R.string.player)),
      onClick = onPlayerClicked,
    )

    SettingsClickLabel(
      text = stringResource(R.string.playback),
      supportingText = stringResource(R.string.playback_settings_and_behaviour),
      onClick = onPlaybackClicked,
    )

    SettingsClickLabel(
      text = stringResource(R.string.notification),
      supportingText = stringResource(R.string.notification_settings_supporting),
      onClick = onNotificationClicked,
    )

    if (uiState.canDelete) {
      SettingsClickLabel(
        text = stringResource(R.string.podcast),
        supportingText = stringResource(R.string.screen_settings, stringResource(R.string.podcast)),
        onClick = onPodcastClicked,
      )
    }

    SettingsClickLabel(
      text = stringResource(R.string.listening_sessions),
      supportingText =
        stringResource(R.string.screen_settings, stringResource(R.string.listening_sessions)),
      onClick = onListeningSessionClicked,
    )
  }
}

@Composable
private fun DisplaySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.display),
  )
  MySwitch(
    modifier = Modifier.padding(start = 24.dp, end = 16.dp),
    title = stringResource(R.string.dark_mode),
    checked = uiState.isDarkMode,
    contentDescription = stringResource(R.string.dark_mode),
    onCheckedChange = { onEvent(SettingsEvent.SwitchDarkTheme(it)) },
  )
  MySwitch(
    modifier = Modifier.padding(start = 24.dp, end = 16.dp),
    title = stringResource(R.string.dynamic_theme),
    checked = uiState.isDynamicTheme,
    contentDescription = stringResource(R.string.dynamic_theme),
    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    onCheckedChange = { onEvent(SettingsEvent.SwitchDynamicTheme(it)) },
  )
}

@Composable
private fun OthersSection(version: String, user: String, uiState: SettingsUiState) {
  TextTitleMedium(
    modifier = Modifier.padding(horizontal = 16.dp),
    text = stringResource(R.string.others),
  )
  SettingsBody(
    modifier = Modifier.padding(start = 24.dp, end = 16.dp),
    text = stringResource(R.string.args_version, version),
  )
  val userText =
    user +
      if (uiState.isAdmin) stringResource(R.string.is_an_admin)
      else stringResource(R.string.is_not_an_admin)
  SettingsBody(modifier = Modifier.padding(start = 24.dp, end = 16.dp), text = userText)
}

@Composable
fun LogoutSection(
  onEvent: (SettingsEvent) -> Unit = {},
  changePassword: () -> Unit,
  initialShowLogoutDialog: Boolean = false,
  initialShowReLoginDialog: Boolean = false,
) {
  var showLogoutDialog by
    remember(initialShowLogoutDialog) { mutableStateOf(initialShowLogoutDialog) }
  var showReLoginDialog by
    remember(initialShowReLoginDialog) { mutableStateOf(initialShowReLoginDialog) }

  Row(modifier = Modifier.padding(horizontal = 16.dp)) {
    TextButton(
      onClick = { changePassword() },
      modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
      Text(stringResource(R.string.change_password))
    }
  }

  Row(modifier = Modifier.padding(horizontal = 16.dp)) {
    TextButton(
      onClick = { showLogoutDialog = true },
      modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
      Text(stringResource(R.string.logout))
    }

    TextButton(
      onClick = { showReLoginDialog = true },
      modifier = Modifier.weight(1f).padding(vertical = 4.dp),
    ) {
      Text(stringResource(R.string.re_login))
    }
  }

  MyAlertDialog(
    title = stringResource(R.string.logout),
    text = stringResource(R.string.dialog_logout_text),
    showDialog = showLogoutDialog,
    confirmText = stringResource(R.string.ok),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onEvent(SettingsEvent.LogoutButtonPressed)
      showLogoutDialog = false
    },
    onDismiss = { showLogoutDialog = false },
  )

  MyAlertDialog(
    title = stringResource(R.string.re_login),
    text = stringResource(R.string.dialog_re_login_text),
    showDialog = showReLoginDialog,
    confirmText = stringResource(R.string.ok),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onEvent(SettingsEvent.ReLoginButtonPressed)
      showReLoginDialog = false
    },
    onDismiss = { showReLoginDialog = false },
  )
}

@ShelfDroidPreview
@Composable
fun SettingsScreenContentPreview() {
  val uiState = SettingsUiState(canDelete = true)
  PreviewWrapper(dynamicColor = false) { SettingsScreenContent(uiState) }
}

@ShelfDroidPreview
@Composable
fun SettingsScreenContentDynamicPreview() {
  val isDynamicTheme = true
  val uiState = SettingsUiState(isDynamicTheme = isDynamicTheme)
  PreviewWrapper(dynamicColor = isDynamicTheme) { SettingsScreenContent(uiState) }
}

@ShelfDroidPreview
@Composable
private fun LogoutSectionLogoutDialogPreview() {
  PreviewWrapper(dynamicColor = false) {
    LogoutSection(changePassword = {}, initialShowLogoutDialog = true)
  }
}

@ShelfDroidPreview
@Composable
private fun LogoutSectionReLoginDialogPreview() {
  PreviewWrapper(dynamicColor = false) {
    LogoutSection(changePassword = {}, initialShowReLoginDialog = true)
  }
}
