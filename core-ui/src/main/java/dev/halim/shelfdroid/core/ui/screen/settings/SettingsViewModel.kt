package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.data.screen.settings.SettingsRepository
import dev.halim.shelfdroid.core.data.screen.settings.SettingsState
import dev.halim.shelfdroid.core.data.screen.settings.SettingsUiState
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel
@Inject
constructor(private val repository: SettingsRepository, @Named("version") val version: String) :
  ViewModel() {

  private val _uiState = MutableStateFlow(SettingsUiState())
  val uiState: StateFlow<SettingsUiState> =
    combine(_uiState, repository.darkMode, repository.dynamicTheme, repository.prefs) {
        uiState: SettingsUiState,
        isDarkMode: Boolean,
        isDynamicTheme: Boolean,
        prefs: Prefs ->
        uiState.copy(
          isDarkMode = isDarkMode,
          isDynamicTheme = isDynamicTheme,
          displayPrefs = prefs.displayPrefs,
          crudPrefs = prefs.crudPrefs,
          isAdmin = prefs.userPrefs.isAdmin,
          username = prefs.userPrefs.username,
        )
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsUiState())

  fun onEvent(event: SettingsEvent) {
    when (event) {
      is SettingsEvent.LogoutButtonPressed -> viewModelScope.launch { logout() }
      is SettingsEvent.SwitchDarkTheme -> {
        viewModelScope.launch { repository.updateDarkMode(event.isDarkMode) }
      }
      is SettingsEvent.SwitchDynamicTheme -> {
        viewModelScope.launch { repository.updateDynamicTheme(event.isDynamic) }
      }
      is SettingsEvent.SwitchListView -> {
        viewModelScope.launch { repository.updateListView(event.isListView) }
      }
      is SettingsEvent.SwitchHardDelete -> {
        viewModelScope.launch { repository.updateHardDelete(event.hardDelete) }
      }

      is SettingsEvent.SettingsDisplayPrefsEvent -> {
        when (event.displayPrefsEvent) {
          is DisplayPrefsEvent.BookSort -> {
            val bookSort = BookSort.fromLabel(event.displayPrefsEvent.bookSort)
            viewModelScope.launch { repository.updateBookSort(bookSort) }
          }
          is DisplayPrefsEvent.Filter -> {
            val filter = Filter.valueOf(event.displayPrefsEvent.filter)
            viewModelScope.launch { repository.updateFilter(filter) }
          }
          is DisplayPrefsEvent.PodcastSort -> {
            val podcastSort = PodcastSort.fromLabel(event.displayPrefsEvent.podcastSort)
            viewModelScope.launch { repository.updatePodcastSort(podcastSort) }
          }
          is DisplayPrefsEvent.PodcastSortOrder -> {
            val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
            viewModelScope.launch { repository.updatePodcastSortOrder(sortOrder) }
          }
          is DisplayPrefsEvent.SortOrder -> {
            val sortOrder = SortOrder.valueOf(event.displayPrefsEvent.sortOrder)
            viewModelScope.launch { repository.updateSortOrder(sortOrder) }
          }
        }
      }
    }
  }

  private suspend fun logout() {
    repository.logout().apply {
      onSuccess { _uiState.update { it.copy(settingsState = SettingsState.Success) } }
      onFailure { error ->
        _uiState.update { it.copy(settingsState = SettingsState.Failure(error.message)) }
      }
    }
  }
}

sealed class SettingsEvent {
  data object LogoutButtonPressed : SettingsEvent()

  data class SwitchDarkTheme(val isDarkMode: Boolean) : SettingsEvent()

  data class SwitchDynamicTheme(val isDynamic: Boolean) : SettingsEvent()

  data class SwitchListView(val isListView: Boolean) : SettingsEvent()

  data class SwitchHardDelete(val hardDelete: Boolean) : SettingsEvent()

  data class SettingsDisplayPrefsEvent(val displayPrefsEvent: DisplayPrefsEvent) : SettingsEvent()
}
