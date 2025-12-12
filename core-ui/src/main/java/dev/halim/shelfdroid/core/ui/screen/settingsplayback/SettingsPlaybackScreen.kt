package dev.halim.shelfdroid.core.ui.screen.settingsplayback

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.settingsplayback.SettingsPlaybackUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsLabel
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSublabel
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSwitchItem

@Composable
fun SettingsPlaybackScreen(viewModel: SettingsPlaybackViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsPlaybackContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
private fun SettingsPlaybackContent(
  uiState: SettingsPlaybackUiState = SettingsPlaybackUiState(),
  onEvent: (SettingsPlaybackEvent) -> Unit = {},
) {

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    BehaviourSection(uiState, onEvent)
  }
}

@Composable
private fun BehaviourSection(
  uiState: SettingsPlaybackUiState,
  onEvent: (SettingsPlaybackEvent) -> Unit = {},
) {
  SettingsLabel(text = stringResource(R.string.behaviour))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_book_and_podcast))
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_playback_speed),
    checked = uiState.keepSpeed,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchKeepSpeed(it)) },
    contentDescription = stringResource(R.string.keep_playback_speed),
  )
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_sleep_timer),
    checked = uiState.keepSleepTimer,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchKeepSleepTimer(it)) },
    contentDescription = stringResource(R.string.keep_sleep_timer),
  )
  Spacer(modifier = Modifier.height(12.dp))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_podcast_episodes))
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_playback_speed),
    checked = uiState.episodeKeepSpeed,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchEpisodeKeepSpeed(it)) },
    contentDescription = stringResource(R.string.keep_playback_speed),
  )
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_sleep_timer),
    checked = uiState.episodeKeepSleepTimer,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchEpisodeKeepSleepTimer(it)) },
    contentDescription = stringResource(R.string.keep_sleep_timer),
  )

  Spacer(modifier = Modifier.height(12.dp))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_books))
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_playback_speed),
    checked = uiState.bookKeepSpeed,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchBookKeepSpeed(it)) },
    contentDescription = stringResource(R.string.keep_playback_speed),
  )
  SettingsSwitchItem(
    modifier = Modifier.padding(start = 16.dp),
    title = stringResource(R.string.keep_sleep_timer),
    checked = uiState.bookKeepSleepTimer,
    onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchBookKeepSleepTimer(it)) },
    contentDescription = stringResource(R.string.keep_sleep_timer),
  )
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsPlaybackContent() }
}
