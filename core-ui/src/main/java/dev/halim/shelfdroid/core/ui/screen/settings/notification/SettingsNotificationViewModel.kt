package dev.halim.shelfdroid.core.ui.screen.settings.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationRepository
import dev.halim.shelfdroid.core.data.screen.settings.notification.SettingsNotificationUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsNotificationViewModel
@Inject
constructor(private val repository: SettingsNotificationRepository) : ViewModel() {

  val uiState: StateFlow<SettingsNotificationUiState> =
    repository.notificationPrefs
      .map { SettingsNotificationUiState(sleepTimerMinutes = it.sleepTimerMinutes) }
      .stateIn(viewModelScope, SharingStarted.Lazily, SettingsNotificationUiState())

  fun onEvent(event: SettingsNotificationEvent) {
    when (event) {
      is SettingsNotificationEvent.ChangeSleepTimerMinutes ->
        viewModelScope.launch { repository.updateDefaultSleepTimerMinutes(event.minutes) }
    }
  }
}

sealed interface SettingsNotificationEvent {
  data class ChangeSleepTimerMinutes(val minutes: Int) : SettingsNotificationEvent
}
