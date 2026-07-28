package dev.halim.shelfdroid.core.ui.screen.apprisenotificationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsMutationTarget
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsRepository
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.AppriseNotificationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.NotificationRuleForm
import dev.halim.shelfdroid.core.data.screen.apprisenotificationsettings.validateNotificationRule
import dev.halim.shelfdroid.core.navigation.NavEditAppriseNotificationRule
import dev.halim.shelfdroid.core.ui.navigation.EditAppriseNotificationRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = NotificationRuleEditorViewModel.Factory::class)
class NotificationRuleEditorViewModel
@AssistedInject
constructor(
  @Assisted navKey: EditAppriseNotificationRule,
  private val repository: AppriseNotificationSettingsRepository,
) : ViewModel() {

  private val _uiState = MutableStateFlow(AppriseNotificationSettingsUiState())
  val uiState: StateFlow<AppriseNotificationSettingsUiState> =
    _uiState.stateIn(
      viewModelScope,
      SharingStarted.WhileSubscribed(5000L),
      AppriseNotificationSettingsUiState(),
    )

  private val _form = MutableStateFlow(navKey.payload.toForm())
  val form: StateFlow<NotificationRuleForm> =
    _form.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), _form.value)

  init {
    loadSettings()
  }

  fun updateForm(transform: (NotificationRuleForm) -> NotificationRuleForm) {
    _form.update(transform)
  }

  fun save() {
    if (_uiState.value.state !is GenericState.Success || !validateNotificationRule(_form.value).isValid) {
      return
    }

    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AppriseNotificationSettingsApiState.Loading(
              AppriseNotificationSettingsMutationTarget.NotificationRule,
            ),
        )
      }
      _uiState.update { repository.mutateRule(it, _form.value) }
    }
  }

  private fun loadSettings() {
    viewModelScope.launch { _uiState.update { repository.load() } }
  }

  @AssistedFactory
  interface Factory {
    fun create(navKey: EditAppriseNotificationRule): NotificationRuleEditorViewModel
  }
}

private fun NavEditAppriseNotificationRule.toForm() =
  NotificationRuleForm(
    id = id,
    libraryId = libraryId,
    eventName = eventName,
    urls = urls,
    titleTemplate = titleTemplate,
    bodyTemplate = bodyTemplate,
    enabled = enabled,
    type = type,
  )
