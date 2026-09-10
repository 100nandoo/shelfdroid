package dev.halim.shelfdroid.core.ui.screen.settings.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.data.screen.settings.home.SettingsHomeUiState
import dev.halim.shelfdroid.core.prefs.BookSort
import dev.halim.shelfdroid.core.prefs.Filter
import dev.halim.shelfdroid.core.prefs.PodcastSort
import dev.halim.shelfdroid.core.prefs.Prefs
import dev.halim.shelfdroid.core.prefs.SortOrder
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsHomeViewModel
@Inject
constructor(private val settingsRepository: SettingsRepository) : ViewModel() {
  private val _uiState = MutableStateFlow(SettingsHomeUiState())
  val uiState: StateFlow<SettingsHomeUiState> =
    combine(_uiState, settingsRepository.prefs) { uiState: SettingsHomeUiState, prefs: Prefs ->
        uiState.copy(
          displayPrefs = prefs.displayPrefs,
          crudPrefs = prefs.crudPrefs,
          canDelete = prefs.userPrefs.isAdmin || prefs.userPrefs.delete,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsHomeUiState())

  fun onEvent(event: SettingsHomeEvent) {
    when (event) {
      is SettingsHomeEvent.SwitchListView -> {
        viewModelScope.launch { settingsRepository.updateListView(event.isListView) }
      }
      is SettingsHomeEvent.SwitchHardDelete -> {
        viewModelScope.launch { settingsRepository.updateHardDelete(event.hardDelete) }
      }
      is SettingsHomeEvent.DisplayPrefs -> {
        when (val prefsEvent = event.event) {
          is DisplayPrefsEvent.BookSort -> {
            viewModelScope.launch {
              settingsRepository.updateBookSort(BookSort.fromLabel(prefsEvent.bookSort))
            }
          }
          is DisplayPrefsEvent.Filter -> {
            viewModelScope.launch {
              settingsRepository.updateFilter(Filter.valueOf(prefsEvent.filter))
            }
          }
          is DisplayPrefsEvent.PodcastSort -> {
            viewModelScope.launch {
              settingsRepository.updatePodcastSort(PodcastSort.fromLabel(prefsEvent.podcastSort))
            }
          }
          is DisplayPrefsEvent.PodcastSortOrder -> {
            viewModelScope.launch {
              settingsRepository.updatePodcastSortOrder(SortOrder.valueOf(prefsEvent.sortOrder))
            }
          }
          is DisplayPrefsEvent.SortOrder -> {
            viewModelScope.launch {
              settingsRepository.updateSortOrder(SortOrder.valueOf(prefsEvent.sortOrder))
            }
          }
        }
      }
    }
  }
}

sealed interface SettingsHomeEvent {
  data class SwitchListView(val isListView: Boolean) : SettingsHomeEvent

  data class SwitchHardDelete(val hardDelete: Boolean) : SettingsHomeEvent

  data class DisplayPrefs(val event: DisplayPrefsEvent) : SettingsHomeEvent
}
