package dev.halim.shelfdroid.core.ui.screen.settingsplayback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.PlaybackPrefs
import dev.halim.shelfdroid.core.data.screen.settingsplayback.SettingsPlaybackRepository
import dev.halim.shelfdroid.core.data.screen.settingsplayback.SettingsPlaybackUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPlaybackViewModel
@Inject
constructor(private val repository: SettingsPlaybackRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsPlaybackUiState())

  val uiState: StateFlow<SettingsPlaybackUiState> =
    combine(_uiState, repository.playbackPrefs) {
        uiState: SettingsPlaybackUiState,
        playbackPrefs: PlaybackPrefs ->
        uiState.copy(
          keepSpeed = playbackPrefs.keepSpeed,
          keepSleepTimer = playbackPrefs.keepSleepTimer,
          episodeKeepSpeed = playbackPrefs.episodeKeepSpeed,
          episodeKeepSleepTimer = playbackPrefs.episodeKeepSleepTimer,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsPlaybackUiState())

  fun onEvent(event: SettingsPlaybackEvent) {
    when (event) {
      is SettingsPlaybackEvent.SwitchKeepSpeed ->
        viewModelScope.launch { repository.updateKeepSpeed(event.keepSpeed) }
      is SettingsPlaybackEvent.SwitchKeepSleepTimer ->
        viewModelScope.launch { repository.updateKeepSleepTimer(event.keepSleepTimer) }
      is SettingsPlaybackEvent.SwitchEpisodeKeepSpeed ->
        viewModelScope.launch { repository.updateEpisodeKeepSpeed(event.episodeKeepSpeed) }
      is SettingsPlaybackEvent.SwitchEpisodeKeepSleepTimer ->
        viewModelScope.launch {
          repository.updateEpisodeKeepSleepTimer(event.episodeKeepSleepTimer)
        }
    }
  }
}

sealed class SettingsPlaybackEvent {
  data class SwitchKeepSpeed(val keepSpeed: Boolean) : SettingsPlaybackEvent()

  data class SwitchKeepSleepTimer(val keepSleepTimer: Boolean) : SettingsPlaybackEvent()

  data class SwitchEpisodeKeepSpeed(val episodeKeepSpeed: Boolean) : SettingsPlaybackEvent()

  data class SwitchEpisodeKeepSleepTimer(val episodeKeepSleepTimer: Boolean) :
    SettingsPlaybackEvent()
}
