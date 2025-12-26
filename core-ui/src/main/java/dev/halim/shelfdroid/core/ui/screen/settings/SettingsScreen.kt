package dev.halim.shelfdroid.core.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExposedDropdownMenu
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsScreen(
  viewModel: SettingsViewModel = hiltViewModel(),
  onPlaybackClicked: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val version = remember { viewModel.version }
  SettingsScreenContent(
    uiState = uiState,
    version = version,
    user = uiState.username,
    onPlaybackClicked = onPlaybackClicked,
    { settingsEvent -> viewModel.onEvent(settingsEvent) },
  )
}

@Composable
fun SettingsScreenContent(
  uiState: SettingsUiState = SettingsUiState(),
  version: String = Defaults.VERSION,
  user: String = Defaults.USERNAME,
  onPlaybackClicked: () -> Unit = {},
  onEvent: (SettingsEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    LogoutSection(onEvent)
    Spacer(modifier = Modifier.height(16.dp))

    OthersSection(version, user, uiState)
    Spacer(modifier = Modifier.height(16.dp))

    DisplaySection(uiState, onEvent)
    HomeScreenSection(uiState, onEvent)
    Spacer(modifier = Modifier.height(16.dp))

    SettingsClickLabel(
      text = stringResource(R.string.playback),
      supportingText = stringResource(R.string.playback_settings_and_behaviour),
      onClick = onPlaybackClicked,
    )
  }
}

@Composable
private fun DisplaySection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
  SettingsLabel(text = stringResource(R.string.display))
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.dark_mode),
    checked = uiState.isDarkMode,
    contentDescription = stringResource(R.string.dark_mode),
    onCheckedChange = { onEvent(SettingsEvent.SwitchDarkTheme(it)) },
  )
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.dynamic_theme),
    checked = uiState.isDynamicTheme,
    contentDescription = stringResource(R.string.dynamic_theme),
    enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
    onCheckedChange = { onEvent(SettingsEvent.SwitchDynamicTheme(it)) },
  )
}

@Composable
private fun HomeScreenSection(uiState: SettingsUiState, onEvent: (SettingsEvent) -> Unit) {
  SettingsLabel(Modifier.padding(top = 16.dp), text = stringResource(R.string.home_screen))
  val paddingStart = Modifier.padding(start = 8.dp)
  SettingsSwitchItem(
    modifier = paddingStart,
    title = stringResource(R.string.list_view),
    checked = uiState.displayPrefs.listView,
    contentDescription = stringResource(R.string.list_view),
    onCheckedChange = { onEvent(SettingsEvent.SwitchListView(it)) },
  )
  SettingsSwitchItem(
    modifier = paddingStart,
    title = stringResource(R.string.show_only_downloaded),
    checked = uiState.displayPrefs.filter.isDownloaded(),
    contentDescription = stringResource(R.string.show_only_downloaded),
    onCheckedChange = {
      val filter = if (it) Filter.Downloaded else Filter.All
      onEvent(SettingsEvent.SettingsDisplayPrefsEvent(DisplayPrefsEvent.Filter(filter.name)))
    },
  )
  val paddingStartTwo = Modifier.padding(start = 16.dp, top = 4.dp)
  SettingsSublabel(
    Modifier.padding(start = 8.dp, top = 4.dp),
    text = stringResource(R.string.book_library),
  )
  Row(modifier = paddingStartTwo.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    ExposedDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.sort),
      options = BookSort.entries.map { it.label },
      initialValue = uiState.displayPrefs.bookSort.label,
      onClick = { onEvent(SettingsEvent.SettingsDisplayPrefsEvent(DisplayPrefsEvent.BookSort(it))) },
    )
    Spacer(Modifier.width(8.dp))
    ExposedDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.order),
      options = SortOrder.entries.map { it.name },
      initialValue = uiState.displayPrefs.sortOrder.name,
      onClick = {
        onEvent(SettingsEvent.SettingsDisplayPrefsEvent(DisplayPrefsEvent.SortOrder(it)))
      },
    )
  }

  SettingsSublabel(
    Modifier.padding(start = 8.dp, top = 4.dp),
    text = stringResource(R.string.podcast_library),
  )
  Row(modifier = paddingStartTwo.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    ExposedDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.sort),
      options = PodcastSort.entries.map { it.label },
      initialValue = uiState.displayPrefs.podcastSort.label,
      onClick = {
        onEvent(SettingsEvent.SettingsDisplayPrefsEvent(DisplayPrefsEvent.PodcastSort(it)))
      },
    )
    Spacer(Modifier.width(8.dp))
    ExposedDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.order),
      options = SortOrder.entries.map { it.name },
      initialValue = uiState.displayPrefs.podcastSortOrder.name,
      onClick = {
        onEvent(SettingsEvent.SettingsDisplayPrefsEvent(DisplayPrefsEvent.PodcastSortOrder(it)))
      },
    )
  }
}

@Composable
private fun OthersSection(version: String, user: String, uiState: SettingsUiState) {
  SettingsLabel(text = stringResource(R.string.others))
  SettingsBody(
    modifier = Modifier.padding(start = 8.dp),
    text = stringResource(R.string.args_version, version),
  )
  val userText =
    user +
      if (uiState.isAdmin) stringResource(R.string.is_an_admin)
      else stringResource(R.string.is_not_an_admin)
  SettingsBody(modifier = Modifier.padding(start = 8.dp), text = userText)
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
