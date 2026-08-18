package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsConfirmation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsRepository
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidationError
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsOperation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.canSave
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.hasChanges
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.isOpenIdConfigurationValid
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.validation
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class AuthenticationSettingsViewModel
internal constructor(
  private val loadOperation: suspend () -> AuthenticationSettingsUiState,
  private val saveOperation: suspend (AuthenticationSettingsUiState) -> AuthenticationSettingsUiState,
  private val discoverOperation: suspend (AuthenticationSettingsUiState) -> AuthenticationSettingsUiState =
    { it },
) : ViewModel() {

  @Inject
  constructor(repository: AuthenticationSettingsRepository) : this(
    loadOperation = { repository.load() },
    saveOperation = { repository.save(it) },
    discoverOperation = { repository.discover(it) },
  )

  private val _uiState = MutableStateFlow(AuthenticationSettingsUiState())
  val uiState: StateFlow<AuthenticationSettingsUiState> =
    _uiState
      .onStart { load() }
      .stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000L),
        AuthenticationSettingsUiState(),
      )

  fun onEvent(event: AuthenticationSettingsEvent) {
    when (event) {
      AuthenticationSettingsEvent.Retry -> load()
      AuthenticationSettingsEvent.DiscoverOpenId,
      AuthenticationSettingsEvent.AutoPopulateOpenId,
      AuthenticationSettingsEvent.DiscoverIssuer -> discover()
      is AuthenticationSettingsEvent.UpdateDraftSettings -> updateDraft(event.transform)
      is AuthenticationSettingsEvent.UpdateCustomMessage ->
        updateDraft {
          it.copy(
            customMessage = event.message,
            customMessageEnabled = event.message.isNotEmpty(),
          )
        }
      is AuthenticationSettingsEvent.SetCustomMessageEnabled ->
        updateDraft {
          if (event.enabled) it.copy(customMessageEnabled = true)
          else it.copy(customMessageEnabled = false, customMessage = "")
        }
      is AuthenticationSettingsEvent.SetOpenIdLoginEnabled ->
        updateDraft { draft -> draft.withLoginMethod(LoginMethod.OpenId, event.enabled) }
      is AuthenticationSettingsEvent.SetPasswordSignInEnabled ->
        setPasswordSignInEnabled(event.enabled)
      AuthenticationSettingsEvent.ConfirmDisablePasswordSignIn -> confirmDisablePasswordSignIn()
      AuthenticationSettingsEvent.DismissConfirmation ->
        _uiState.update { it.copy(pendingConfirmation = null) }
      AuthenticationSettingsEvent.ResetDraftSettings -> resetDraft()
      AuthenticationSettingsEvent.Reset -> resetDraft()
      AuthenticationSettingsEvent.SaveSettings,
      AuthenticationSettingsEvent.Save -> save()
      AuthenticationSettingsEvent.RequestBack -> requestBack()
      AuthenticationSettingsEvent.ConfirmLeave -> confirmLeave()
      AuthenticationSettingsEvent.ConsumeLeaveRequest ->
        _uiState.update { it.copy(leaveRequested = false) }
    }
  }

  private fun load() {
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          state = AuthenticationSettingsState.Loading,
          apiState = AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Load),
          pendingConfirmation = null,
          leaveRequested = false,
          restartRequired = false,
          signingAlgorithmOptions = emptyList(),
        )
      }
      _uiState.update { loadOperation() }
    }
  }

  private fun updateDraft(transform: (AuthenticationSettingsSummary) -> AuthenticationSettingsSummary) {
    _uiState.update { current ->
      if (current.apiState is AuthenticationSettingsApiState.Loading) return@update current
      val draft = current.draftSettings ?: return@update current
      val updated = transform(draft)
      current.copy(
        state = AuthenticationSettingsState.Ready(updated),
        draftSettings = updated,
        validation = updated.validation(),
        apiState = AuthenticationSettingsApiState.Idle,
        leaveRequested = false,
      )
    }
  }

  private fun setPasswordSignInEnabled(enabled: Boolean) {
    if (enabled) {
      updateDraft { it.withLoginMethod(LoginMethod.Local, true) }
      return
    }

    val draft = _uiState.value.draftSettings ?: return
    if (
      LoginMethod.OpenId !in draft.activeLoginMethods ||
        !draft.isOpenIdConfigurationValid()
    ) {
      _uiState.update {
        it.copy(
          validation =
            AuthenticationSettingsValidation(
              setOf(AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete)
            )
        )
      }
      return
    }
    _uiState.update {
      it.copy(pendingConfirmation = AuthenticationSettingsConfirmation.DisablePasswordSignIn)
    }
  }

  private fun confirmDisablePasswordSignIn() {
    val draft = _uiState.value.draftSettings ?: return
    if (
      LoginMethod.OpenId !in draft.activeLoginMethods ||
        !draft.isOpenIdConfigurationValid()
    ) {
      setPasswordSignInEnabled(false)
      return
    }
    updateDraft { it.withLoginMethod(LoginMethod.Local, false) }
    _uiState.update { it.copy(pendingConfirmation = null) }
  }

  private fun resetDraft() {
    _uiState.update { current ->
      val saved = current.savedSettings ?: return@update current
      current.copy(
        state = AuthenticationSettingsState.Ready(saved),
        draftSettings = saved,
        validation = saved.validation(),
        apiState = AuthenticationSettingsApiState.Idle,
        signingAlgorithmOptions = emptyList(),
        pendingConfirmation = null,
        leaveRequested = false,
      )
    }
  }

  private fun save() {
    if (!_uiState.value.canSave) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AuthenticationSettingsApiState.Loading(
              AuthenticationSettingsOperation.Save
            ),
          pendingConfirmation = null,
        )
      }
      _uiState.update { saveOperation(it) }
    }
  }

  private fun discover() {
    val initial = _uiState.value
    if (initial.apiState is AuthenticationSettingsApiState.Loading) return
    if (initial.draftSettings == null) return
    viewModelScope.launch {
      _uiState.update {
        it.copy(
          apiState =
            AuthenticationSettingsApiState.Loading(
              AuthenticationSettingsOperation.Discovery
            ),
          pendingConfirmation = null,
        )
      }
      val discovered = discoverOperation(initial)
      _uiState.update { current ->
        // A discovery operation receives an immutable snapshot. If a caller changed the draft
        // while the request was in flight, keep that newer draft instead of applying stale data.
        if (current.draftSettings != initial.draftSettings) {
          current.copy(
            apiState = discovered.apiState,
            state = current.draftSettings?.let(AuthenticationSettingsState::Ready)
              ?: current.state,
          )
        } else {
          discovered
        }
      }
    }
  }

  private fun requestBack() {
    _uiState.update { current ->
      if (current.hasChanges) {
        current.copy(
          pendingConfirmation = AuthenticationSettingsConfirmation.LeaveWithUnsavedChanges
        )
      } else {
        current.copy(leaveRequested = true)
      }
    }
  }

  private fun confirmLeave() {
    _uiState.update {
      it.copy(
        pendingConfirmation = null,
        apiState = AuthenticationSettingsApiState.Idle,
        leaveRequested = true,
      )
    }
  }
}

private fun AuthenticationSettingsSummary.withLoginMethod(
  method: LoginMethod,
  enabled: Boolean,
): AuthenticationSettingsSummary {
  val methods = activeLoginMethods.toMutableList()
  if (enabled && method !in methods) methods += method
  if (!enabled) methods.remove(method)
  return copy(activeLoginMethods = methods)
}

sealed interface AuthenticationSettingsEvent {
  data object Retry : AuthenticationSettingsEvent

  data object DiscoverOpenId : AuthenticationSettingsEvent

  data object AutoPopulateOpenId : AuthenticationSettingsEvent

  data object DiscoverIssuer : AuthenticationSettingsEvent

  data class UpdateDraftSettings(
    val transform: (AuthenticationSettingsSummary) -> AuthenticationSettingsSummary,
  ) : AuthenticationSettingsEvent

  data class UpdateCustomMessage(val message: String) : AuthenticationSettingsEvent

  data class SetCustomMessageEnabled(val enabled: Boolean) : AuthenticationSettingsEvent

  data class SetPasswordSignInEnabled(val enabled: Boolean) : AuthenticationSettingsEvent

  data object ConfirmDisablePasswordSignIn : AuthenticationSettingsEvent

  data class SetOpenIdLoginEnabled(val enabled: Boolean) : AuthenticationSettingsEvent

  data object ResetDraftSettings : AuthenticationSettingsEvent

  data object SaveSettings : AuthenticationSettingsEvent

  data object Save : AuthenticationSettingsEvent

  data object Reset : AuthenticationSettingsEvent

  data object RequestBack : AuthenticationSettingsEvent

  data object ConfirmLeave : AuthenticationSettingsEvent

  data object DismissConfirmation : AuthenticationSettingsEvent

  data object ConsumeLeaveRequest : AuthenticationSettingsEvent
}
