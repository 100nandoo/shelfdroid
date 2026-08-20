package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject

class AuthenticationSettingsRepository @Inject constructor(private val api: ApiService) {

  suspend fun load(): AuthenticationSettingsUiState {
    return api
      .authenticationSettings()
      .fold(
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
    val saved = uiState.savedSettings ?: return uiState
    val draft = uiState.draftSettings ?: return uiState
    val validation = draft.validation(callbackSubfolderOptions = uiState.callbackSubfolderOptions)
    if (!validation.isValid) {
      return uiState.copy(validation = validation)
    }
    val request =
      AuthenticationSettingsMapper.toUpdateRequest(saved, draft)
        ?: return uiState.copy(apiState = AuthenticationSettingsApiState.Idle)
    val restartRequired = AuthenticationSettingsMapper.hasOpenIdChanges(saved, draft)

    val response =
      api.updateAuthenticationSettings(request).getOrElse { error ->
        return uiState.copy(
          apiState =
            AuthenticationSettingsApiState.Failure(
              AuthenticationSettingsOperation.Save,
              error.message,
            )
        )
      }

    if (!response.updated) {
      return uiState.copy(apiState = AuthenticationSettingsApiState.Rejected)
    }

    val canonical = load()
    return when (canonical.state) {
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
        )
    }
  }

  suspend fun discover(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState {
    val start = uiState.draftSettings ?: AuthenticationSettingsSummary()
    return discover(uiState, start)
  }

  suspend fun discover(
    uiState: AuthenticationSettingsUiState,
    operationStart: AuthenticationSettingsSummary,
  ): AuthenticationSettingsUiState {
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

    val response =
      api.openIdIssuerConfiguration(issuer).getOrElse { error ->
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
}

private fun Throwable.toAuthenticationSettingsState(): AuthenticationSettingsState =
  AuthenticationSettingsState.Failure(message)

private fun callbackSubfolderOptions(settings: AuthenticationSettingsSummary): List<String> =
  (callbackSubfolderOptions(DataStoreManager.BASE_URL) +
      settings.openId.subfolderForRedirectUrls.takeIf { it.isNotBlank() }.orEmpty())
    .distinct()
