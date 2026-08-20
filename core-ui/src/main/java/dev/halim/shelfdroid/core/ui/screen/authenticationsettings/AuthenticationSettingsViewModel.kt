package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsConfirmation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsOperation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsRepository
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidationError
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class AuthenticationSettingsViewModel
internal constructor(
  private val loadOperation: suspend () -> AuthenticationSettingsUiState,
  private val saveOperation:
    suspend (AuthenticationSettingsUiState) -> AuthenticationSettingsUiState,
  private val discoverOperation:
    suspend (AuthenticationSettingsUiState) -> AuthenticationSettingsUiState =
    {
      it
    },
) : ViewModel() {

  @Inject
  constructor(
    repository: AuthenticationSettingsRepository
  ) : this(
    loadOperation = { repository.load() },
    saveOperation = { repository.save(it) },
    discoverOperation = { repository.discover(it) },
  )

  private val _uiState = MutableStateFlow(AuthenticationSettingsUiState())
  private var pendingMobileRedirectUris: List<String>? = null
  private var pendingShelfDroidCallbackWarning = false
  private var pendingWildcardConfirmation = false
  private val operationMutex = Mutex()
  private var operationGeneration = 0L
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
      is AuthenticationSettingsEvent.UpdateClientSecret -> updateClientSecret(event.value)
      is AuthenticationSettingsEvent.AddMobileRedirectUri ->
        requestMobileRedirectUpdate(
          _uiState.value.draftSettings?.openId?.mobileRedirectUris.orEmpty() + event.uri
        )
      is AuthenticationSettingsEvent.UpdateMobileRedirectUri ->
        updateMobileRedirectUri(event.index, event.uri)
      is AuthenticationSettingsEvent.RemoveMobileRedirectUri -> removeMobileRedirectUri(event.index)
      is AuthenticationSettingsEvent.SetCallbackSubfolder ->
        updateDraft { it.copy(openId = it.openId.copy(subfolderForRedirectUrls = event.value)) }
      AuthenticationSettingsEvent.ConfirmRemoveShelfDroidCallback ->
        confirmRemoveShelfDroidCallback()
      AuthenticationSettingsEvent.ConfirmWildcardMobileRedirect -> confirmWildcardMobileRedirect()
      AuthenticationSettingsEvent.DismissConfirmation -> dismissConfirmation()
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
    clearPendingMobileRedirect()
    val generation = beginOperation(AuthenticationSettingsOperation.Load)
    viewModelScope.launch {
      operationMutex.withLock {
        val loaded = loadOperation()
        if (generation == operationGeneration) {
          _uiState.update { loaded }
        }
      }
    }
  }

  private fun updateDraft(
    transform: (AuthenticationSettingsSummary) -> AuthenticationSettingsSummary
  ) {
    _uiState.update { current ->
      if (current.apiState is AuthenticationSettingsApiState.Loading) return@update current
      val draft = current.draftSettings ?: return@update current
      val updated = transform(draft)
      current.copy(
        state = AuthenticationSettingsState.Ready(updated),
        draftSettings = updated,
        validation =
          updated.validation(callbackSubfolderOptions = current.callbackSubfolderOptions),
        apiState = AuthenticationSettingsApiState.Idle,
        leaveRequested = false,
      )
    }
  }

  private fun setPasswordSignInEnabled(enabled: Boolean) {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    if (enabled) {
      updateDraft { it.withLoginMethod(LoginMethod.Local, true) }
      return
    }

    val draft = _uiState.value.draftSettings ?: return
    if (LoginMethod.OpenId !in draft.activeLoginMethods || !draft.isOpenIdConfigurationValid()) {
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
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    val draft = _uiState.value.draftSettings ?: return
    if (LoginMethod.OpenId !in draft.activeLoginMethods || !draft.isOpenIdConfigurationValid()) {
      setPasswordSignInEnabled(false)
      return
    }
    updateDraft { it.withLoginMethod(LoginMethod.Local, false) }
    _uiState.update { it.copy(pendingConfirmation = null) }
  }

  private fun updateClientSecret(value: String) {
    updateDraft { summary ->
      summary.copy(openId = summary.openId.copy(clientSecret = value))
    }
  }

  private fun resetDraft() {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    clearPendingMobileRedirect()
    _uiState.update { current ->
      val saved = current.savedSettings ?: return@update current
      current.copy(
        state = AuthenticationSettingsState.Ready(saved),
        draftSettings = saved,
        validation = saved.validation(callbackSubfolderOptions = current.callbackSubfolderOptions),
        apiState = AuthenticationSettingsApiState.Idle,
        signingAlgorithmOptions = emptyList(),
        pendingConfirmation = null,
        leaveRequested = false,
      )
    }
  }

  private fun save() {
    if (!_uiState.value.canSave) return
    val snapshot = _uiState.value
    val generation = beginOperation(AuthenticationSettingsOperation.Save)
    viewModelScope.launch {
      operationMutex.withLock {
        val result = saveOperation(snapshot)
        clearPendingMobileRedirect()
        if (generation == operationGeneration) {
          _uiState.update { result }
        }
      }
    }
  }

  private fun discover() {
    val initial = _uiState.value
    if (initial.apiState is AuthenticationSettingsApiState.Loading) return
    if (initial.draftSettings == null) return
    val generation = beginOperation(AuthenticationSettingsOperation.Discovery)
    viewModelScope.launch {
      operationMutex.withLock {
        val discovered = discoverOperation(initial)
        if (generation == operationGeneration) {
          _uiState.update { current ->
            // A discovery operation receives an immutable snapshot. If a caller changed the draft
            // while the request was in flight, keep that newer draft instead of applying stale
            // data.
            if (current.draftSettings != initial.draftSettings) {
              current.copy(
                apiState = discovered.apiState,
                state =
                  current.draftSettings?.let(AuthenticationSettingsState::Ready) ?: current.state,
              )
            } else {
              discovered
            }
          }
        }
      }
    }
  }

  private fun beginOperation(operation: AuthenticationSettingsOperation): Long {
    operationGeneration += 1
    _uiState.update {
      it.copy(
        state =
          if (operation == AuthenticationSettingsOperation.Load) {
            AuthenticationSettingsState.Loading
          } else {
            it.draftSettings?.let(AuthenticationSettingsState::Ready) ?: it.state
          },
        apiState = AuthenticationSettingsApiState.Loading(operation),
        pendingConfirmation = null,
        leaveRequested = false,
        restartRequired =
          if (operation == AuthenticationSettingsOperation.Load) false else it.restartRequired,
        signingAlgorithmOptions =
          if (operation == AuthenticationSettingsOperation.Load) emptyList()
          else it.signingAlgorithmOptions,
      )
    }
    return operationGeneration
  }

  private fun requestBack() {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
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
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    clearPendingMobileRedirect()
    _uiState.update {
      it.copy(
        pendingConfirmation = null,
        apiState = AuthenticationSettingsApiState.Idle,
        leaveRequested = true,
      )
    }
  }

  private fun updateMobileRedirectUri(index: Int, value: String) {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    val current = _uiState.value.draftSettings?.openId?.mobileRedirectUris ?: return
    if (index !in current.indices) return
    val updated = current.toMutableList().also { it[index] = value }
    if (
      (current[index] == SHELFDROID_CALLBACK_URI && value != SHELFDROID_CALLBACK_URI) ||
        (value == "*" && current.size == 1 && current.single() != "*")
    ) {
      requestMobileRedirectUpdate(updated)
    } else {
      updateDraft { it.copy(openId = it.openId.copy(mobileRedirectUris = updated)) }
    }
  }

  private fun removeMobileRedirectUri(index: Int) {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    val current = _uiState.value.draftSettings?.openId?.mobileRedirectUris ?: return
    if (index !in current.indices) return
    requestMobileRedirectUpdate(current.toMutableList().also { it.removeAt(index) })
  }

  private fun requestMobileRedirectUpdate(next: List<String>) {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    val current = _uiState.value.draftSettings?.openId?.mobileRedirectUris ?: return
    if (next == current) return

    pendingMobileRedirectUris = next
    pendingShelfDroidCallbackWarning =
      SHELFDROID_CALLBACK_URI in current && SHELFDROID_CALLBACK_URI !in next
    pendingWildcardConfirmation = "*" in next && next.size == 1 && "*" !in current
    when {
      pendingShelfDroidCallbackWarning ->
        _uiState.update {
          it.copy(pendingConfirmation = AuthenticationSettingsConfirmation.RemoveShelfDroidCallback)
        }
      pendingWildcardConfirmation ->
        _uiState.update {
          it.copy(
            pendingConfirmation = AuthenticationSettingsConfirmation.UseWildcardMobileRedirect
          )
        }
      else -> applyPendingMobileRedirectUris()
    }
  }

  private fun confirmRemoveShelfDroidCallback() {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    if (pendingMobileRedirectUris == null) return
    pendingShelfDroidCallbackWarning = false
    if (pendingWildcardConfirmation) {
      _uiState.update {
        it.copy(pendingConfirmation = AuthenticationSettingsConfirmation.UseWildcardMobileRedirect)
      }
    } else {
      applyPendingMobileRedirectUris()
    }
  }

  private fun confirmWildcardMobileRedirect() {
    if (_uiState.value.apiState is AuthenticationSettingsApiState.Loading) return
    if (pendingMobileRedirectUris == null) return
    pendingWildcardConfirmation = false
    if (pendingShelfDroidCallbackWarning) {
      _uiState.update {
        it.copy(pendingConfirmation = AuthenticationSettingsConfirmation.RemoveShelfDroidCallback)
      }
    } else {
      applyPendingMobileRedirectUris()
    }
  }

  private fun applyPendingMobileRedirectUris() {
    val next = pendingMobileRedirectUris ?: return
    clearPendingMobileRedirect()
    updateDraft { it.copy(openId = it.openId.copy(mobileRedirectUris = next)) }
  }

  private fun dismissConfirmation() {
    clearPendingMobileRedirect()
    _uiState.update { it.copy(pendingConfirmation = null) }
  }

  private fun clearPendingMobileRedirect() {
    pendingMobileRedirectUris = null
    pendingShelfDroidCallbackWarning = false
    pendingWildcardConfirmation = false
  }
}

private const val SHELFDROID_CALLBACK_URI = "audiobookshelf://oauth"

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
    val transform: (AuthenticationSettingsSummary) -> AuthenticationSettingsSummary
  ) : AuthenticationSettingsEvent

  data class UpdateCustomMessage(val message: String) : AuthenticationSettingsEvent

  data class SetCustomMessageEnabled(val enabled: Boolean) : AuthenticationSettingsEvent

  data class SetPasswordSignInEnabled(val enabled: Boolean) : AuthenticationSettingsEvent

  data object ConfirmDisablePasswordSignIn : AuthenticationSettingsEvent

  data class UpdateClientSecret(val value: String) : AuthenticationSettingsEvent

  data class AddMobileRedirectUri(val uri: String) : AuthenticationSettingsEvent

  data class UpdateMobileRedirectUri(val index: Int, val uri: String) : AuthenticationSettingsEvent

  data class RemoveMobileRedirectUri(val index: Int) : AuthenticationSettingsEvent

  data class SetCallbackSubfolder(val value: String) : AuthenticationSettingsEvent

  data object ConfirmRemoveShelfDroidCallback : AuthenticationSettingsEvent

  data object ConfirmWildcardMobileRedirect : AuthenticationSettingsEvent

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
