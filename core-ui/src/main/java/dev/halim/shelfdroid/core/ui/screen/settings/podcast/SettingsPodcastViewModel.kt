package dev.halim.shelfdroid.core.ui.screen.settings.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.settings.podcast.SettingsPodcastUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPodcastViewModel @Inject constructor(private val prefsRepository: PrefsRepository) :
  ViewModel() {
  private val _uiState = MutableStateFlow(SettingsPodcastUiState())
  val uiState: StateFlow<SettingsPodcastUiState> =
    combine(_uiState, prefsRepository.prefsFlow()) { uiState: SettingsPodcastUiState, prefs: Prefs
        ->
        uiState.copy(crudPrefs = prefs.crudPrefs, userPrefs = prefs.userPrefs)
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsPodcastUiState())

  fun onEvent(event: SettingsPodcastEvent) {
    when (event) {
      is SettingsPodcastEvent.SwitchHardDelete -> {
        viewModelScope.launch { prefsRepository.updateHardDelete(event.hardDelete) }
      }
      is SettingsPodcastEvent.SwitchAutoSelectFinished -> {
        viewModelScope.launch { prefsRepository.updateAutoSelectFinished(event.enabled) }
      }
      is SettingsPodcastEvent.SwitchHideDownloaded -> {
        viewModelScope.launch { prefsRepository.updateHideDownloaded(event.hideDownloaded) }
      }
    }
  }
}

sealed interface SettingsPodcastEvent {
  data class SwitchHardDelete(val hardDelete: Boolean) : SettingsPodcastEvent

  data class SwitchAutoSelectFinished(val enabled: Boolean) : SettingsPodcastEvent

  data class SwitchHideDownloaded(val hideDownloaded: Boolean) : SettingsPodcastEvent
}
