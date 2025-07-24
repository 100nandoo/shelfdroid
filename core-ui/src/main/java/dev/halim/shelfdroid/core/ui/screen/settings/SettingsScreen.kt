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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.data.screen.settings.SettingsState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), onLogoutSuccess: () -> Unit) {
  val uiState by viewModel.uiState.collectAsState()
  val version = remember { viewModel.version }
  SettingsScreenContent(
    uiState = uiState,
    version = version,
    user = uiState.username,
    { settingsEvent -> viewModel.onEvent(settingsEvent) },
    onLogoutSuccess,
  )
}

@Composable
fun SettingsScreenContent(
  uiState: SettingsUiState = SettingsUiState(),
  version: String = Defaults.VERSION,
  user: String = Defaults.USERNAME,
  onEvent: (SettingsEvent) -> Unit = {},
  onLogoutSuccess: () -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
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

    TextButton(
      onClick = { onEvent(SettingsEvent.LogoutButtonPressed) },
      modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
      Text(stringResource(R.string.logout))
    }

    LaunchedEffect(uiState.settingsState) {
      if (uiState.settingsState == SettingsState.Success) {
        onLogoutSuccess()
      }
    }
  }
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
