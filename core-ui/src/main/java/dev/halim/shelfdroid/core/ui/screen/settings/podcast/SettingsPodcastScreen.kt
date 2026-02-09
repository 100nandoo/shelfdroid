package dev.halim.shelfdroid.core.ui.screen.settings.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.CrudPrefs
import dev.halim.shelfdroid.core.UserPrefs
import dev.halim.shelfdroid.core.data.screen.settings.podcast.SettingsPodcastUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsLabel
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSwitchItem

@Composable
fun SettingsPodcastScreen(viewModel: SettingsPodcastViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  SettingsPodcastScreenContent(uiState, viewModel::onEvent)
}

@Composable
private fun SettingsPodcastScreenContent(
  uiState: SettingsPodcastUiState = SettingsPodcastUiState(),
  onEvent: (SettingsPodcastEvent) -> Unit = {},
) {
  val canAdd by remember(uiState.userPrefs.isAdmin) { mutableStateOf(uiState.userPrefs.isAdmin) }
  val canDelete by
    remember(uiState.userPrefs.isAdmin, uiState.userPrefs.delete) {
      mutableStateOf(uiState.userPrefs.delete || uiState.userPrefs.isAdmin)
    }
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    if (canDelete) {
      SettingsLabel(text = stringResource(R.string.delete))
      SettingsSwitchItem(
        modifier = Modifier.padding(start = 8.dp),
        title = stringResource(R.string.user_permanent_deletion),
        checked = uiState.crudPrefs.episodeHardDelete,
        contentDescription = stringResource(R.string.user_permanent_deletion),
        onCheckedChange = { onEvent(SettingsPodcastEvent.SwitchHardDelete(it)) },
      )
      SettingsSwitchItem(
        modifier = Modifier.padding(start = 8.dp),
        title = stringResource(R.string.auto_select_finished_episodes),
        checked = uiState.crudPrefs.episodeAutoSelectFinished,
        contentDescription = stringResource(R.string.auto_select_finished_episodes),
        onCheckedChange = { onEvent(SettingsPodcastEvent.SwitchHardDelete(it)) },
      )
    }
    Spacer(modifier = Modifier.height(16.dp))

    if (canAdd) {
      SettingsLabel(text = stringResource(R.string.add_episode))
      SettingsSwitchItem(
        modifier = Modifier.padding(start = 8.dp),
        title = stringResource(R.string.hide_downloaded_episodes),
        checked = uiState.crudPrefs.addEpisodeHideDownloaded,
        contentDescription = stringResource(R.string.hide_downloaded_episodes),
        onCheckedChange = { onEvent(SettingsPodcastEvent.SwitchHideDownloaded(it)) },
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun SettingsPodcastScreenContentPreview() {
  val uiState =
    SettingsPodcastUiState(
      crudPrefs = CrudPrefs(hardDelete = true),
      userPrefs = UserPrefs(isAdmin = true),
    )
  PreviewWrapper(dynamicColor = false) { SettingsPodcastScreenContent(uiState) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val uiState =
    SettingsPodcastUiState(
      crudPrefs = CrudPrefs(hardDelete = true),
      userPrefs = UserPrefs(delete = true),
    )
  PreviewWrapper(dynamicColor = true) { SettingsPodcastScreenContent(uiState) }
}
