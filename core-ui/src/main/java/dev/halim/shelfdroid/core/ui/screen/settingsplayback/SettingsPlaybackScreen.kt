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
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSublabel
import dev.halim.shelfdroid.core.ui.screen.settings.search.SettingsSearchSettingId
import dev.halim.shelfdroid.core.ui.screen.settings.search.SettingsSearchTarget

@Composable
fun SettingsPlaybackScreen(
  viewModel: SettingsPlaybackViewModel = hiltViewModel(),
  searchTargetId: String? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsPlaybackContent(uiState, searchTargetId) { event -> viewModel.onEvent(event) }
}

@Composable
private fun SettingsPlaybackContent(
  uiState: SettingsPlaybackUiState = SettingsPlaybackUiState(),
  searchTargetId: String? = null,
  onEvent: (SettingsPlaybackEvent) -> Unit = {},
) {

  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    BehaviourSection(uiState, onEvent, searchTargetId)
  }
}

@Composable
private fun BehaviourSection(
  uiState: SettingsPlaybackUiState,
  onEvent: (SettingsPlaybackEvent) -> Unit = {},
  searchTargetId: String?,
) {
  TextTitleMedium(text = stringResource(R.string.behaviour))
  Spacer(modifier = Modifier.height(16.dp))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_book_and_podcast))
  val startPadding = Modifier.padding(start = 8.dp)
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_SPEED_BETWEEN_TYPES,
    label = stringResource(R.string.keep_playback_speed),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_playback_speed),
      checked = uiState.keepSpeed,
      contentDescription = stringResource(R.string.keep_playback_speed),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchKeepSpeed(it)) },
    )
  }
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_TIMER_BETWEEN_TYPES,
    label = stringResource(R.string.keep_sleep_timer),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_sleep_timer),
      checked = uiState.keepSleepTimer,
      contentDescription = stringResource(R.string.keep_sleep_timer),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchKeepSleepTimer(it)) },
    )
  }
  Spacer(modifier = Modifier.height(12.dp))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_podcast_episodes))
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_SPEED_BETWEEN_EPISODES,
    label = stringResource(R.string.keep_playback_speed),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_playback_speed),
      checked = uiState.episodeKeepSpeed,
      contentDescription = stringResource(R.string.keep_playback_speed),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchEpisodeKeepSpeed(it)) },
    )
  }
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_TIMER_BETWEEN_EPISODES,
    label = stringResource(R.string.keep_sleep_timer),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_sleep_timer),
      checked = uiState.episodeKeepSleepTimer,
      contentDescription = stringResource(R.string.keep_sleep_timer),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchEpisodeKeepSleepTimer(it)) },
    )
  }

  Spacer(modifier = Modifier.height(12.dp))
  SettingsSublabel(text = stringResource(R.string.when_switching_between_books))
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_SPEED_BETWEEN_BOOKS,
    label = stringResource(R.string.keep_playback_speed),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_playback_speed),
      checked = uiState.bookKeepSpeed,
      contentDescription = stringResource(R.string.keep_playback_speed),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchBookKeepSpeed(it)) },
    )
  }
  SettingsSearchTarget(
    settingId = SettingsSearchSettingId.KEEP_TIMER_BETWEEN_BOOKS,
    label = stringResource(R.string.keep_sleep_timer),
    targetSettingId = searchTargetId,
  ) {
    MySwitch(
      modifier = startPadding,
      title = stringResource(R.string.keep_sleep_timer),
      checked = uiState.bookKeepSleepTimer,
      contentDescription = stringResource(R.string.keep_sleep_timer),
      onCheckedChange = { onEvent(SettingsPlaybackEvent.SwitchBookKeepSleepTimer(it)) },
    )
  }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsPlaybackContent() }
}
