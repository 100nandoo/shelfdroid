package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseGlobalSettingsForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleUi
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.notificationruleeditor.AppriseNotificationSettingsRepository
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

  fun onEvent(event: AppriseNotificationSettingsEvent) {
    when (event) {
      is AppriseNotificationSettingsEvent.UpdateDraftSettings -> {
        _uiState.update { it.copy(draftSettings = event.transform(it.draftSettings)) }
      }
      AppriseNotificationSettingsEvent.Refresh -> initialPage()
      AppriseNotificationSettingsEvent.SaveSettings -> saveSettings()
      is AppriseNotificationSettingsEvent.SaveRule -> saveRule(event.form)
      is AppriseNotificationSettingsEvent.DeleteRule -> deleteRule(event.rule)
      is AppriseNotificationSettingsEvent.TestRule -> testRule(event.rule)
    }
  }

  private fun initialPage() {
    viewModelScope.launch { _uiState.update { repository.load() } }
  }

  private fun saveSettings() {
    if (!_uiState.value.canSave) return

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.GlobalSettings
            )
        )
      }
      _uiState.update { repository.saveSettings(it) }
    }
  }

  private fun saveRule(form: NotificationRuleForm) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.NotificationRule
            )
        )
      }
      _uiState.update { repository.mutateRule(it, form) }
    }
  }

  private fun deleteRule(rule: NotificationRuleUi) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.NotificationRuleDelete
            )
        )
      }
      _uiState.update { repository.deleteRule(it, rule) }
    }
  }

  private fun testRule(rule: NotificationRuleUi) {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.NotificationRuleTest
            )
        )
      }
      _uiState.update { repository.testRule(it, rule) }
    }
  }
}

sealed interface AppriseNotificationSettingsEvent {
  data class UpdateDraftSettings(
    val transform: (AppriseGlobalSettingsForm) -> AppriseGlobalSettingsForm
  ) : AppriseNotificationSettingsEvent

  data object SaveSettings : AppriseNotificationSettingsEvent

  data object Refresh : AppriseNotificationSettingsEvent

  data class SaveRule(val form: NotificationRuleForm) : AppriseNotificationSettingsEvent

  data class DeleteRule(val rule: NotificationRuleUi) : AppriseNotificationSettingsEvent

  data class TestRule(val rule: NotificationRuleUi) : AppriseNotificationSettingsEvent
}
