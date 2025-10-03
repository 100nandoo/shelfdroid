package dev.halim.shelfdroid.core.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val version = remember { viewModel.version }
  SettingsScreenContent(
    uiState = uiState,
    version = version,
    user = uiState.username,
    { settingsEvent -> viewModel.onEvent(settingsEvent) },
  )
}

@Composable
fun SettingsScreenContent(
  uiState: SettingsUiState = SettingsUiState(),
  version: String = Defaults.VERSION,
  user: String = Defaults.USERNAME,
  onEvent: (SettingsEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    DisplaySection(uiState, onEvent)

    Spacer(modifier = Modifier.height(16.dp))

    SettingsLabel(text = stringResource(R.string.others))
    Spacer(modifier = Modifier.height(4.dp))
    SettingsBody(text = stringResource(R.string.args_version, version))
    val userText =
      user +
        if (uiState.isAdmin) stringResource(R.string.is_an_admin)
        else stringResource(R.string.is_not_an_admin)
    SettingsBody(text = userText)

    Spacer(modifier = Modifier.height(16.dp))

    LogoutSection(onEvent)
  }
}

@Composable
private fun DisplaySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
  SettingsLabel(text = stringResource(R.string.display))
  Spacer(modifier = Modifier.height(4.dp))
  SettingsSwitchItem(
    title = stringResource(R.string.dark_mode),
    checked = uiState.isDarkMode,
    onCheckedChange = { onEvent(SettingsEvent.SwitchDarkTheme(it)) },
    contentDescription = stringResource(R.string.dark_mode),
  )
  SettingsSwitchItem(
    title = stringResource(R.string.dynamic_theme),
    checked = uiState.isDynamicTheme,
    onCheckedChange = { onEvent(SettingsEvent.SwitchDynamicTheme(it)) },
    contentDescription = stringResource(R.string.dynamic_theme),
    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
  )
  SettingsSublabel(Modifier.padding(top = 16.dp), text = stringResource(R.string.home_screen))
  SettingsSwitchItem(
    modifier = Modifier,
    title = stringResource(R.string.list_view),
    checked = uiState.isListView,
    onCheckedChange = { onEvent(SettingsEvent.SwitchListView(it)) },
    contentDescription = stringResource(R.string.list_view),
  )
  SettingsSwitchItem(
    modifier = Modifier,
    title = stringResource(R.string.show_only_downloaded),
    checked = uiState.isOnlyDownloaded,
    onCheckedChange = {
      val filter = if (it) Filter.Downloaded else Filter.All
      onEvent(SettingsEvent.SwitchFilter(filter))
    },
    contentDescription = stringResource(R.string.show_only_downloaded),
  )
}

@Composable
fun LogoutSection(onEvent: (SettingsEvent) -> Unit = {}) {
  var showLogoutDialog by remember { mutableStateOf(false) }

  TextButton(
    onClick = { showLogoutDialog = true },
    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
  ) {
    Text(stringResource(R.string.logout))
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
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsScreenContent() }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val isDynamicTheme = true
  val uiState = SettingsUiState(isDynamicTheme = isDynamicTheme)
  PreviewWrapper(dynamicColor = isDynamicTheme) { SettingsScreenContent(uiState) }
}
