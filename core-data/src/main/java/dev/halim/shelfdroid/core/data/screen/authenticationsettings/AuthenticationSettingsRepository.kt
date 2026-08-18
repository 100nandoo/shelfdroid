package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.data.admin.AdminDestinationGuard
import javax.inject.Inject
import retrofit2.HttpException

class AuthenticationSettingsRepository
@Inject
constructor(
  private val api: ApiService,
  private val adminDestinationGuard: AdminDestinationGuard,
) {

  suspend fun load(): AuthenticationSettingsUiState {
    if (!adminDestinationGuard.canAccess()) {
      return AuthenticationSettingsUiState(AuthenticationSettingsState.AccessDenied)
    }

    return api.authenticationSettings().fold(
      onSuccess = { response ->
        val settings = AuthenticationSettingsMapper.map(response)
        val allowedSubfolders = callbackSubfolderOptions(settings)
        AuthenticationSettingsUiState(
          state = AuthenticationSettingsState.Ready(settings),
          savedSettings = settings,
          draftSettings = settings,
          callbackSubfolderOptions = allowedSubfolders,
          serverBaseUrl = DataStoreManager.BASE_URL,
          validation = settings.validation(callbackSubfolderOptions = allowedSubfolders),
        )
      },
      onFailure = { error ->
        AuthenticationSettingsUiState(
          state = error.toAuthenticationSettingsState(),
          apiState =
            AuthenticationSettingsApiState.Failure(
              AuthenticationSettingsOperation.Load,
              error.message,
            ),
        )
      },
    )
  }

  suspend fun save(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState {
    return save(uiState, AuthenticationSettingsSecretUpdate.Untouched)
  }

  suspend fun save(
    uiState: AuthenticationSettingsUiState,
    secretUpdate: AuthenticationSettingsSecretUpdate,
  ): AuthenticationSettingsUiState {
    if (!adminDestinationGuard.canAccess()) {
      return uiState.copy(
        state = AuthenticationSettingsState.AccessDenied,
        savedSettings = null,
        draftSettings = null,
        signingAlgorithmOptions = emptyList(),
        callbackSubfolderOptions = listOf(""),
        pendingConfirmation = null,
        restartRequired = false,
        clientSecretChangePending = false,
      )
    }

    val saved = uiState.savedSettings ?: return uiState
    val draft = uiState.draftSettings ?: return uiState
    val validation = draft.validation(secretUpdate, uiState.callbackSubfolderOptions)
    if (!validation.isValid) {
      return uiState.copy(validation = validation)
    }
    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft, secretUpdate)
      ?: return uiState.copy(apiState = AuthenticationSettingsApiState.Idle)
    val restartRequired = AuthenticationSettingsMapper.hasOpenIdChanges(saved, draft, secretUpdate)

    val response =
      api.updateAuthenticationSettings(request).getOrElse { error ->
        if (error is HttpException && error.code() == 403) {
          return uiState.copy(
            state = AuthenticationSettingsState.AccessDenied,
            savedSettings = null,
            draftSettings = null,
            signingAlgorithmOptions = emptyList(),
            pendingConfirmation = null,
            restartRequired = false,
            clientSecretChangePending = false,
          )
        }
        return uiState.copy(
          apiState =
            AuthenticationSettingsApiState.Failure(
              AuthenticationSettingsOperation.Save,
              error.message,
            ),
          clientSecretChangePending = false,
        )
      }

    if (!response.updated) {
      return uiState.copy(
        apiState = AuthenticationSettingsApiState.Rejected,
        clientSecretChangePending = false,
      )
    }

    val canonical = load()
    return when (canonical.state) {
      AuthenticationSettingsState.AccessDenied -> canonical
      is AuthenticationSettingsState.Failure ->
        canonical.copy(
          apiState =
            AuthenticationSettingsApiState.Failure(
              AuthenticationSettingsOperation.Save,
              canonical.state.message,
            )
        )
      AuthenticationSettingsState.Loading -> canonical
      is AuthenticationSettingsState.Ready ->
        canonical.copy(
          apiState = AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save),
          signingAlgorithmOptions = uiState.signingAlgorithmOptions,
          restartRequired = uiState.restartRequired || restartRequired,
          clientSecretChangePending = false,
        )
    }
  }

  /**
   * Fetches provider metadata through Audiobookshelf. The server owns the request to the
   * identity provider, so this call remains authenticated and respects the configured base path.
   */
  suspend fun discover(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState {
    val start = uiState.draftSettings ?: AuthenticationSettingsSummary()
    return discover(uiState, start)
  }

  suspend fun discover(
    uiState: AuthenticationSettingsUiState,
    operationStart: AuthenticationSettingsSummary,
  ): AuthenticationSettingsUiState {
    if (!adminDestinationGuard.canAccess()) {
      return uiState.copy(
        state = AuthenticationSettingsState.AccessDenied,
        savedSettings = null,
        draftSettings = null,
        signingAlgorithmOptions = emptyList(),
        callbackSubfolderOptions = listOf(""),
        pendingConfirmation = null,
        restartRequired = false,
        clientSecretChangePending = false,
      )
    }

    val draft = uiState.draftSettings ?: return uiState
    val issuer = operationStart.openId.issuerUrl.trim()
    if (issuer.isEmpty()) {
      return uiState.copy(
        state = AuthenticationSettingsState.Ready(draft),
        apiState =
          AuthenticationSettingsApiState.Failure(
            AuthenticationSettingsOperation.Discovery,
            "Issuer URL is required.",
          ),
      )
    }

    val response = api.openIdIssuerConfiguration(issuer).getOrElse { error ->
      if (error is HttpException && error.code() == 403) {
        return uiState.copy(
          state = AuthenticationSettingsState.AccessDenied,
          savedSettings = null,
          draftSettings = null,
          signingAlgorithmOptions = emptyList(),
          callbackSubfolderOptions = listOf(""),
          pendingConfirmation = null,
          restartRequired = false,
          clientSecretChangePending = false,
        )
      }
      return uiState.copy(
        state = AuthenticationSettingsState.Ready(draft),
        apiState =
          AuthenticationSettingsApiState.Failure(
            AuthenticationSettingsOperation.Discovery,
            error.message,
          ),
      )
    }

    val discovery = AuthenticationSettingsMapper.mapDiscovery(response)
    val merged =
      AuthenticationSettingsMapper.mergeDiscovery(
        current = draft,
        operationStart = operationStart,
        discovery = discovery,
      )
    return uiState.copy(
      state = AuthenticationSettingsState.Ready(merged),
      draftSettings = merged,
      signingAlgorithmOptions = discovery.signingAlgorithms,
      validation = merged.validation(callbackSubfolderOptions = uiState.callbackSubfolderOptions),
      apiState = AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Discovery),
    )
  }

  suspend fun discoverIssuer(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState =
    discover(uiState)

  suspend fun saveSettings(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState =
    save(uiState)
}

private fun Throwable.toAuthenticationSettingsState(): AuthenticationSettingsState =
  if (this is HttpException && code() == 403) {
    AuthenticationSettingsState.AccessDenied
  } else {
    AuthenticationSettingsState.Failure(message)
  }

private fun callbackSubfolderOptions(settings: AuthenticationSettingsSummary): List<String> =
  (callbackSubfolderOptions(DataStoreManager.BASE_URL) +
      settings.openId.subfolderForRedirectUrls.takeIf { it.isNotBlank() }.orEmpty())
    .distinct()
