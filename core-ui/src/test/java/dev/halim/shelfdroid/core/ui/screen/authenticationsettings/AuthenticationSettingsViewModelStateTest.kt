package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsConfirmation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsOperation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidationError
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OpenIdSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.canSave
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.hasChanges
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.validation
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticationSettingsViewModelStateTest {

  @Test
  fun cleanState_cannotSave() {
    val settings = settings()
    val state = readyState(settings, settings)

    assertFalse(state.hasChanges)
    assertFalse(state.canSave)
  }

  @Test
  fun validDirtyState_canSave() {
    val saved = settings()
    val draft = saved.copy(customMessage = "<p>Changed</p>")
    val state = readyState(saved, draft)

    assertTrue(state.hasChanges)
    assertTrue(state.canSave)
  }

  @Test
  fun invalidDraft_cannotSave() {
    val saved = settings()
    val draft = saved.copy(activeLoginMethods = emptyList())
    val state = readyState(saved, draft)

    assertTrue(AuthenticationSettingsValidationError.NoLoginMethod in state.validation.errors)
    assertFalse(state.canSave)
  }

  @Test
  fun editClearAndReset_areHandledByViewModel() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    val viewModel = viewModelWith(original)
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("<p>Changed</p>"))
    assertTrue(viewModel.uiState.value.draftSettings?.customMessage == "<p>Changed</p>")

    viewModel.onEvent(AuthenticationSettingsEvent.SetCustomMessageEnabled(false))
    assertFalse(viewModel.uiState.value.draftSettings?.customMessageEnabled == true)
    assertTrue(viewModel.uiState.value.draftSettings?.customMessage.isNullOrEmpty())

    viewModel.onEvent(
      AuthenticationSettingsEvent.UpdateDraftSettings { it.copy(activeLoginMethods = emptyList()) }
    )
    assertTrue(
      AuthenticationSettingsValidationError.NoLoginMethod in viewModel.uiState.value.validation.errors
    )
    assertFalse(viewModel.uiState.value.canSave)

    viewModel.onEvent(AuthenticationSettingsEvent.ResetDraftSettings)
    assertEquals(original, viewModel.uiState.value.draftSettings)
    collection.cancelAndJoin()
  }

  @Test
  fun loadAndCleanSave_skipNoOpUpdate() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    var loadCount = 0
    var saveCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = {
          loadCount += 1
          readyState(original, original)
        },
        saveOperation = {
          saveCount += 1
          it
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    assertEquals(1, loadCount)
    assertFalse(viewModel.uiState.value.canSave)
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()
    assertEquals(0, saveCount)
    collection.cancelAndJoin()
  }

  @Test
  fun passwordDisable_requiresConfirmationAndValidOpenId() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original =
      settings().copy(
        activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
        openId = validOpenId(),
      )
    val viewModel = viewModelWith(original)
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.SetPasswordSignInEnabled(false))
    assertEquals(
      AuthenticationSettingsConfirmation.DisablePasswordSignIn,
      viewModel.uiState.value.pendingConfirmation,
    )
    assertEquals(original.activeLoginMethods, viewModel.uiState.value.draftSettings?.activeLoginMethods)

    viewModel.onEvent(AuthenticationSettingsEvent.ConfirmDisablePasswordSignIn)
    assertTrue(LoginMethod.Local !in viewModel.uiState.value.draftSettings!!.activeLoginMethods)
    collection.cancelAndJoin()

    val incomplete = original.copy(openId = OpenIdSettingsSummary())
    val blocked = viewModelWith(incomplete)
    val blockedCollection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { blocked.uiState.collect {} }
    advanceUntilIdle()
    blocked.onEvent(AuthenticationSettingsEvent.SetPasswordSignInEnabled(false))
    assertEquals(null, blocked.uiState.value.pendingConfirmation)
    assertTrue(
      AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete in
        blocked.uiState.value.validation.errors
    )
    blockedCollection.cancelAndJoin()
  }

  @Test
  fun unsavedBack_warnsThenRequestsLeaveAndSaveUsesCanonicalState() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    var saveCount = 0
    val canonical = original.copy(customMessage = "<p>Canonical</p>")
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { state ->
          saveCount += 1
          state.copy(
            state = AuthenticationSettingsState.Ready(canonical),
            savedSettings = canonical,
            draftSettings = canonical,
            validation = canonical.validation(),
            apiState = AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save),
          )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("<p>Draft</p>"))
    viewModel.onEvent(AuthenticationSettingsEvent.RequestBack)
    assertEquals(
      AuthenticationSettingsConfirmation.LeaveWithUnsavedChanges,
      viewModel.uiState.value.pendingConfirmation,
    )
    viewModel.onEvent(AuthenticationSettingsEvent.ConfirmLeave)
    assertTrue(viewModel.uiState.value.leaveRequested)
    assertEquals(AuthenticationSettingsApiState.Idle, viewModel.uiState.value.apiState)

    viewModel.onEvent(AuthenticationSettingsEvent.ConsumeLeaveRequest)
    assertFalse(viewModel.uiState.value.leaveRequested)
    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("<p>Draft</p>"))
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()
    assertEquals(1, saveCount)
    assertEquals(canonical, viewModel.uiState.value.savedSettings)
    collection.cancelAndJoin()
  }

  @Test
  fun rejectedSaveOutcome_isSurfaced() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    var saveCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { state ->
          saveCount += 1
          state.copy(apiState = AuthenticationSettingsApiState.Rejected)
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("<p>Changed</p>"))
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()

    assertEquals(1, saveCount)
    assertEquals(AuthenticationSettingsApiState.Rejected, viewModel.uiState.value.apiState)
    collection.cancelAndJoin()
  }

  private fun readyState(
    saved: AuthenticationSettingsSummary,
    draft: AuthenticationSettingsSummary,
  ): AuthenticationSettingsUiState =
    AuthenticationSettingsUiState(
      state = AuthenticationSettingsState.Ready(draft),
      savedSettings = saved,
      draftSettings = draft,
      validation = draft.validation(),
    )

  private fun settings() =
    AuthenticationSettingsSummary(
      activeLoginMethods = listOf(LoginMethod.Local),
      customMessage = "<p>Welcome</p>",
      customMessageEnabled = true,
    )

  private fun validOpenId() =
    OpenIdSettingsSummary(
      issuerUrl = "https://issuer.example",
      authorizationUrl = "https://issuer.example/authorize",
      tokenUrl = "https://issuer.example/token",
      userInfoUrl = "https://issuer.example/userinfo",
      jwksUrl = "https://issuer.example/jwks",
      clientId = "client-id",
      tokenSigningAlgorithm = "RS256",
    )

  private fun viewModelWith(settings: AuthenticationSettingsSummary): AuthenticationSettingsViewModel =
    AuthenticationSettingsViewModel(
      loadOperation = { readyState(settings, settings) },
      saveOperation = { it },
    )
}
