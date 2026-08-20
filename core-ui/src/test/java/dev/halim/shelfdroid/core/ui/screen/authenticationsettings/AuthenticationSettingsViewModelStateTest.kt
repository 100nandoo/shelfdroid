package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsConfirmation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsOperation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidationError
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OpenIdSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.canSave
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.hasChanges
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.validation
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
      AuthenticationSettingsValidationError.NoLoginMethod in
        viewModel.uiState.value.validation.errors
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
      settings()
        .copy(
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
    assertEquals(
      original.activeLoginMethods,
      viewModel.uiState.value.draftSettings?.activeLoginMethods,
    )

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
  fun resetAfterOidcSave_preservesRestartRequiredWarning() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings().copy(openId = validOpenId())
    val canonical = original.copy(openId = original.openId.copy(clientId = "saved-client"))
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { state ->
          state.copy(
            state = AuthenticationSettingsState.Ready(canonical),
            savedSettings = canonical,
            draftSettings = canonical,
            validation = canonical.validation(),
            apiState = AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save),
            restartRequired = true,
          )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(
      AuthenticationSettingsEvent.UpdateDraftSettings {
        it.copy(openId = it.openId.copy(clientId = "edited-client"))
      }
    )
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()

    assertFalse(viewModel.uiState.value.hasChanges)
    assertTrue(viewModel.uiState.value.restartRequired)
    viewModel.onEvent(AuthenticationSettingsEvent.ResetDraftSettings)
    assertEquals(canonical, viewModel.uiState.value.draftSettings)
    assertTrue(viewModel.uiState.value.restartRequired)
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

  @Test
  fun mappingControls_updateDirtyStateResetAndSave() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original =
      settings()
        .copy(
          openId =
            validOpenId()
              .copy(
                buttonText = "Before OpenID",
                matchExistingBy = "email",
                autoLaunch = true,
                autoRegister = false,
                groupClaim = "groups",
                advancedPermsClaim = "permissions",
                samplePermissions = "{\"download\":false}",
              )
        )
    val canonical =
      original.copy(
        openId =
          original.openId.copy(
            buttonText = "Continue with Acme",
            matchExistingBy = "username",
            autoLaunch = false,
            autoRegister = true,
            groupClaim = "roles",
            advancedPermsClaim = "abspermissions",
          )
      )
    var savedDraft: AuthenticationSettingsSummary? = null
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { state ->
          savedDraft = state.draftSettings
          readyState(canonical, canonical)
            .copy(
              apiState =
                AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save),
              restartRequired = true,
            )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(
      AuthenticationSettingsEvent.UpdateDraftSettings {
        it.copy(
          openId =
            it.openId.copy(
              buttonText = "Continue with Acme",
              matchExistingBy = "username",
              autoLaunch = false,
              autoRegister = true,
              groupClaim = "roles",
              advancedPermsClaim = "abspermissions",
            )
        )
      }
    )
    assertTrue(viewModel.uiState.value.hasChanges)
    assertTrue(viewModel.uiState.value.canSave)
    viewModel.onEvent(AuthenticationSettingsEvent.ResetDraftSettings)
    assertEquals(original, viewModel.uiState.value.draftSettings)
    assertFalse(viewModel.uiState.value.hasChanges)

    viewModel.onEvent(
      AuthenticationSettingsEvent.UpdateDraftSettings {
        it.copy(openId = it.openId.copy(buttonText = "Continue with Acme"))
      }
    )
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()

    assertEquals("Continue with Acme", savedDraft?.openId?.buttonText)
    assertEquals(canonical, viewModel.uiState.value.draftSettings)
    assertTrue(viewModel.uiState.value.restartRequired)
    collection.cancelAndJoin()
  }

  @Test
  fun discoveryEvent_appliesProviderResultAndSigningAlgorithmOptions() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings().copy(openId = validOpenId())
    val discovered =
      original.copy(
        openId =
          original.openId.copy(
            authorizationUrl = "https://issuer.example/discovered-authorize",
            tokenUrl = "https://issuer.example/discovered-token",
          )
      )
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { it },
        discoverOperation = {
          it.copy(
            state = AuthenticationSettingsState.Ready(discovered),
            draftSettings = discovered,
            signingAlgorithmOptions = listOf("RS256", "ES256"),
            validation = discovered.validation(),
            apiState =
              AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery),
          )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.DiscoverOpenId)
    advanceUntilIdle()

    assertEquals(
      "https://issuer.example/discovered-authorize",
      viewModel.uiState.value.draftSettings?.openId?.authorizationUrl,
    )
    assertEquals(listOf("RS256", "ES256"), viewModel.uiState.value.signingAlgorithmOptions)
    assertEquals(
      AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery),
      viewModel.uiState.value.apiState,
    )
    collection.cancelAndJoin()
  }

  @Test
  fun discoveryInFlight_serializesSaveAndDraftEdits() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings().copy(openId = validOpenId())
    val gate = CompletableDeferred<AuthenticationSettingsUiState>()
    var saveCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = {
          saveCount += 1
          it
        },
        discoverOperation = { gate.await() },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.DiscoverOpenId)
    assertEquals(
      AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Discovery),
      viewModel.uiState.value.apiState,
    )
    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("ignored while discovering"))
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    assertEquals(0, saveCount)
    assertEquals(original.customMessage, viewModel.uiState.value.draftSettings?.customMessage)

    gate.complete(
      viewModel.uiState.value.copy(
        state = AuthenticationSettingsState.Ready(original),
        draftSettings = original,
        validation = original.validation(),
        apiState =
          AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery),
      )
    )
    advanceUntilIdle()
    collection.cancelAndJoin()
  }

  @Test
  fun saveInFlight_disablesDuplicateSaveAndSerializesCompletion() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    val saveGate = CompletableDeferred<Unit>()
    var saveCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = {
          saveCount += 1
          saveGate.await()
          it
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateCustomMessage("<p>Changed</p>"))
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    assertEquals(
      AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Save),
      viewModel.uiState.value.apiState,
    )
    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    assertEquals(1, saveCount)

    saveGate.complete(Unit)
    advanceUntilIdle()
    assertEquals(1, saveCount)
    assertEquals(AuthenticationSettingsApiState.Idle, viewModel.uiState.value.apiState)
    collection.cancelAndJoin()
  }

  @Test
  fun staleLoadResult_cannotOverwriteNewerRetry() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val firstLoadGate = CompletableDeferred<Unit>()
    val secondLoadGate = CompletableDeferred<Unit>()
    val first = settings().copy(customMessage = "first")
    val second = settings().copy(customMessage = "second")
    var loadCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = {
          val currentLoad = ++loadCount
          if (currentLoad == 1) firstLoadGate.await() else secondLoadGate.await()
          readyState(
            if (currentLoad == 1) first else second,
            if (currentLoad == 1) first else second,
          )
        },
        saveOperation = { it },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    runCurrent()
    assertEquals(1, loadCount)

    viewModel.onEvent(AuthenticationSettingsEvent.Retry)
    assertEquals(
      AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Load),
      viewModel.uiState.value.apiState,
    )
    firstLoadGate.complete(Unit)
    runCurrent()
    assertEquals(2, loadCount)
    assertEquals(null, viewModel.uiState.value.draftSettings)

    secondLoadGate.complete(Unit)
    advanceUntilIdle()
    assertEquals("second", viewModel.uiState.value.draftSettings?.customMessage)
    collection.cancelAndJoin()
  }

  @Test
  fun staleDiscoveryResult_cannotOverwriteNewerLoad() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val discoveryGate = CompletableDeferred<Unit>()
    val discoveryStarted = CompletableDeferred<Unit>()
    val loadGate = CompletableDeferred<Unit>()
    val original = settings().copy(openId = validOpenId())
    val staleDiscovery = original.copy(customMessage = "stale discovery")
    val latest = original.copy(customMessage = "latest load")
    var loadCount = 0
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = {
          val currentLoad = ++loadCount
          if (currentLoad > 1) loadGate.await()
          readyState(
            if (currentLoad == 1) original else latest,
            if (currentLoad == 1) original else latest,
          )
        },
        saveOperation = { it },
        discoverOperation = {
          discoveryStarted.complete(Unit)
          discoveryGate.await()
          readyState(staleDiscovery, staleDiscovery)
            .copy(
              apiState =
                AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery)
            )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.DiscoverOpenId)
    runCurrent()
    assertTrue(discoveryStarted.isCompleted)
    viewModel.onEvent(AuthenticationSettingsEvent.Retry)
    discoveryGate.complete(Unit)
    runCurrent()
    assertEquals(
      AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Load),
      viewModel.uiState.value.apiState,
    )
    assertEquals(original.customMessage, viewModel.uiState.value.draftSettings?.customMessage)

    loadGate.complete(Unit)
    advanceUntilIdle()
    assertEquals("latest load", viewModel.uiState.value.draftSettings?.customMessage)
    collection.cancelAndJoin()
  }

  @Test
  fun discoveryFailure_preservesEditedClientSecret() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings().copy(openId = validOpenId())
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { it },
        discoverOperation = { state ->
          state.copy(
            apiState =
              AuthenticationSettingsApiState.Failure(
                AuthenticationSettingsOperation.Discovery,
                "Forbidden",
              )
          )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateClientSecret("replacement-secret"))
    viewModel.onEvent(AuthenticationSettingsEvent.DiscoverOpenId)
    advanceUntilIdle()

    val edited = original.copy(openId = original.openId.copy(clientSecret = "replacement-secret"))
    assertEquals(AuthenticationSettingsState.Ready(edited), viewModel.uiState.value.state)
    assertEquals(
      "replacement-secret",
      viewModel.uiState.value.draftSettings?.openId?.clientSecret,
    )
    collection.cancelAndJoin()
  }

  @Test
  fun clientSecretEditPersistsThroughSave() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings().copy(openId = validOpenId())
    var savedState: AuthenticationSettingsUiState? = null
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { readyState(original, original) },
        saveOperation = { state ->
          savedState = state
          val canonical =
            original.copy(
              openId =
                original.openId.copy(clientSecret = state.draftSettings!!.openId.clientSecret)
            )
          readyState(canonical, canonical)
            .copy(
              apiState =
                AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save)
            )
        },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateClientSecret("replacement-secret"))
    assertEquals(
      "replacement-secret",
      viewModel.uiState.value.draftSettings?.openId?.clientSecret,
    )
    assertTrue(viewModel.uiState.value.canSave)

    viewModel.onEvent(AuthenticationSettingsEvent.SaveSettings)
    advanceUntilIdle()

    assertEquals("replacement-secret", savedState?.draftSettings?.openId?.clientSecret)
    assertEquals(
      "replacement-secret",
      viewModel.uiState.value.draftSettings?.openId?.clientSecret,
    )
    assertFalse(viewModel.uiState.value.hasChanges)
    collection.cancelAndJoin()
  }

  @Test
  fun blankClientSecretInvalidatesEnabledOpenIdImmediately() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original =
      settings()
        .copy(
          activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
          openId = validOpenId(),
        )
    val viewModel = viewModelWith(original)
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateClientSecret(""))
    assertEquals("", viewModel.uiState.value.draftSettings?.openId?.clientSecret)
    assertTrue(
      AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete in
        viewModel.uiState.value.validation.errors
    )
    assertFalse(viewModel.uiState.value.canSave)
    collection.cancelAndJoin()
  }

  @Test
  fun mobileRedirectList_supportsAddRemoveAndWarnsBeforeRemovingShelfDroidCallback() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original = settings()
    val viewModel = viewModelWith(original)
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.AddMobileRedirectUri("audiobookshelf://oauth"))
    assertEquals(
      listOf("audiobookshelf://oauth"),
      viewModel.uiState.value.draftSettings?.openId?.mobileRedirectUris,
    )
    viewModel.onEvent(AuthenticationSettingsEvent.RemoveMobileRedirectUri(0))
    assertEquals(
      AuthenticationSettingsConfirmation.RemoveShelfDroidCallback,
      viewModel.uiState.value.pendingConfirmation,
    )
    viewModel.onEvent(AuthenticationSettingsEvent.ConfirmRemoveShelfDroidCallback)
    assertTrue(
      viewModel.uiState.value.draftSettings?.openId?.mobileRedirectUris.orEmpty().isEmpty()
    )
    collection.cancelAndJoin()
  }

  @Test
  fun mobileRedirectWildcard_requiresHighRiskConfirmation() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val viewModel = viewModelWith(settings())
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.AddMobileRedirectUri("*"))
    assertEquals(
      AuthenticationSettingsConfirmation.UseWildcardMobileRedirect,
      viewModel.uiState.value.pendingConfirmation,
    )
    assertTrue(
      viewModel.uiState.value.draftSettings?.openId?.mobileRedirectUris.orEmpty().isEmpty()
    )
    viewModel.onEvent(AuthenticationSettingsEvent.ConfirmWildcardMobileRedirect)
    assertEquals(listOf("*"), viewModel.uiState.value.draftSettings?.openId?.mobileRedirectUris)
    collection.cancelAndJoin()
  }

  @Test
  fun callbackEdits_validateUrisAndRestrictSubfolderChoices() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original =
      settings()
        .copy(
          openId =
            validOpenId()
              .copy(
                mobileRedirectUris = listOf("audiobookshelf://oauth"),
                subfolderForRedirectUrls = "/shelf",
              )
        )
    val state = readyState(original, original).copy(callbackSubfolderOptions = listOf("", "/shelf"))
    val viewModel =
      AuthenticationSettingsViewModel(
        loadOperation = { state },
        saveOperation = { it },
      )
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    val currentDraft = requireNotNull(viewModel.uiState.value.draftSettings)
    val withSecondUri =
      currentDraft.copy(
        openId =
          currentDraft.openId.copy(
            mobileRedirectUris = listOf("audiobookshelf://oauth", "sampleapp://oauth")
          )
      )
    viewModel.onEvent(AuthenticationSettingsEvent.UpdateDraftSettings { withSecondUri })
    viewModel.onEvent(AuthenticationSettingsEvent.UpdateMobileRedirectUri(1, "not a URI"))
    assertTrue(
      AuthenticationSettingsValidationError.InvalidMobileRedirectUri in
        viewModel.uiState.value.validation.errors
    )
    viewModel.onEvent(AuthenticationSettingsEvent.SetCallbackSubfolder("/invented"))
    assertTrue(
      AuthenticationSettingsValidationError.InvalidCallbackSubfolder in
        viewModel.uiState.value.validation.errors
    )
    viewModel.onEvent(AuthenticationSettingsEvent.SetCallbackSubfolder(""))
    assertFalse(
      AuthenticationSettingsValidationError.InvalidCallbackSubfolder in
        viewModel.uiState.value.validation.errors
    )
    collection.cancelAndJoin()
  }

  @Test
  fun resetRestoresSavedClientSecretAndConfirmedBackRequestsLeave() = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val original =
      settings()
        .copy(
          activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
          openId = validOpenId(),
        )
    val viewModel = viewModelWith(original)
    val collection =
      backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) { viewModel.uiState.collect {} }
    advanceUntilIdle()

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateClientSecret("replacement-secret"))
    viewModel.onEvent(AuthenticationSettingsEvent.ResetDraftSettings)
    assertEquals("secret-value", viewModel.uiState.value.draftSettings?.openId?.clientSecret)
    assertFalse(viewModel.uiState.value.hasChanges)

    viewModel.onEvent(AuthenticationSettingsEvent.UpdateClientSecret("replacement-secret"))
    viewModel.onEvent(AuthenticationSettingsEvent.RequestBack)
    viewModel.onEvent(AuthenticationSettingsEvent.ConfirmLeave)
    assertTrue(viewModel.uiState.value.leaveRequested)
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
      clientSecret = "secret-value",
      tokenSigningAlgorithm = "RS256",
    )

  private fun viewModelWith(
    settings: AuthenticationSettingsSummary
  ): AuthenticationSettingsViewModel =
    AuthenticationSettingsViewModel(
      loadOperation = { readyState(settings, settings) },
      saveOperation = { it },
    )
}
