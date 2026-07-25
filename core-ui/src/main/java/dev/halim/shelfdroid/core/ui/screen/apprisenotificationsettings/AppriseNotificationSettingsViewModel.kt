package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsRepository
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AppriseNotificationSettingsViewModel
@Inject
constructor(private val repository: AppriseNotificationSettingsRepository) : ViewModel() {

  private val _uiState = MutableStateFlow(AppriseNotificationSettingsUiState())
  val uiState: StateFlow<AppriseNotificationSettingsUiState> =
    _uiState
      .onStart { initialPage() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        AppriseNotificationSettingsUiState(),
      )

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.load() } }
  }
}
