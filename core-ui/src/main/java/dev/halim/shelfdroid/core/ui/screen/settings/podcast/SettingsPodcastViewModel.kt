package dev.halim.shelfdroid.core.ui.screen.settings.podcast

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.screen.settings.podcast.SettingsPodcastRepository
import dev.halim.shelfdroid.core.data.screen.settings.podcast.SettingsPodcastUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsPodcastViewModel
@Inject
constructor(private val repository: SettingsPodcastRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(SettingsPodcastUiState())
  val uiState: StateFlow<SettingsPodcastUiState> =
    combine(_uiState, repository.prefs) { uiState: SettingsPodcastUiState, prefs: Prefs ->
        uiState.copy(crudPrefs = prefs.crudPrefs)
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsPodcastUiState())

  fun onEvent(event: SettingsPodcastEvent) {
    when (event) {
      is SettingsPodcastEvent.SwitchHardDelete -> {
        viewModelScope.launch { repository.updateHardDelete(event.hardDelete) }
      }
    }
  }
}

sealed interface SettingsPodcastEvent {
  data class SwitchHardDelete(val hardDelete: Boolean) : SettingsPodcastEvent
}
