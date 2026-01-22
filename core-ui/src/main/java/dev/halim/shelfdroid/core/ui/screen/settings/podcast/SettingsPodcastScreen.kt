package dev.halim.shelfdroid.core.ui.screen.settings.podcast

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.CrudPrefs
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
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SettingsLabel(text = stringResource(R.string.delete))
    SettingsSwitchItem(
      title = stringResource(R.string.user_permanent_deletion),
      checked = uiState.crudPrefs.episodeHardDelete,
      contentDescription = stringResource(R.string.user_permanent_deletion),
      onCheckedChange = { onEvent(SettingsPodcastEvent.SwitchHardDelete(it)) },
    )
    Spacer(modifier = Modifier.height(16.dp))
    SettingsLabel(text = stringResource(R.string.add_episode))
    SettingsSwitchItem(
      title = stringResource(R.string.hide_downloaded_episodes),
      checked = uiState.crudPrefs.addEpisodeHideDownloaded,
      contentDescription = stringResource(R.string.hide_downloaded_episodes),
      onCheckedChange = { onEvent(SettingsPodcastEvent.SwitchHideDownloaded(it)) },
    )
  }
}

@ShelfDroidPreview
@Composable
fun SettingsPodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsPodcastScreenContent() }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val uiState = SettingsPodcastUiState(crudPrefs = CrudPrefs(hardDelete = true))
  PreviewWrapper(dynamicColor = true) { SettingsPodcastScreenContent(uiState) }
}
