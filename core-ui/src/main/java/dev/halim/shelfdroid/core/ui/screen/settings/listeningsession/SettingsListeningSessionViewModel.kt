package dev.halim.shelfdroid.core.ui.screen.settings.listeningsession

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.ItemsPerPage
import dev.halim.shelfdroid.core.ListeningSessionPrefs
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.screen.settings.listeningsession.SettingsListeningSessionRepository
import dev.halim.shelfdroid.core.data.screen.settings.listeningsession.SettingsListeningSessionUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsListeningSessionViewModel
@Inject
constructor(
  private val prefsRepository: PrefsRepository,
  private val repository: SettingsListeningSessionRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(SettingsListeningSessionUiState())
  val uiState: StateFlow<SettingsListeningSessionUiState> =
    combine(_uiState, prefsRepository.listeningSessionPrefs) {
        uiState: SettingsListeningSessionUiState,
        listeningSessionPrefs: ListeningSessionPrefs ->
        val users = repository.users()
        uiState.copy(users = users, listeningSessionPrefs = listeningSessionPrefs)
      }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsListeningSessionUiState())

  fun onEvent(event: SettingsListeningSessionEvent) {
    when (event) {
      is SettingsListeningSessionEvent.DefaultUser -> {
        viewModelScope.launch {
          val updated =
            prefsRepository.listeningSessionPrefs.first().copy(defaultUserId = event.userId)
          prefsRepository.updateListeningSessionPrefs(updated)
        }
      }

      is SettingsListeningSessionEvent.ChangeItemsPerPage -> {
        viewModelScope.launch { repository.updateItemsPerPage(event.itemsPerPage.label) }
      }
    }
  }
}

sealed interface SettingsListeningSessionEvent {

  data class DefaultUser(val userId: String?) : SettingsListeningSessionEvent

  data class ChangeItemsPerPage(val itemsPerPage: ItemsPerPage) : SettingsListeningSessionEvent
}
