package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.ApiService
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
        AuthenticationSettingsUiState(
          state = AuthenticationSettingsState.Ready(settings),
          savedSettings = settings,
          draftSettings = settings,
          validation = settings.validation(),
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
    if (!adminDestinationGuard.canAccess()) {
      return uiState.copy(
        state = AuthenticationSettingsState.AccessDenied,
        savedSettings = null,
        draftSettings = null,
        pendingConfirmation = null,
      )
    }

    val saved = uiState.savedSettings ?: return uiState
    val draft = uiState.draftSettings ?: return uiState
    val request = AuthenticationSettingsMapper.toUpdateRequest(saved, draft)
      ?: return uiState.copy(apiState = AuthenticationSettingsApiState.Idle)

    val response =
      api.updateAuthenticationSettings(request).getOrElse { error ->
        if (error is HttpException && error.code() == 403) {
          return uiState.copy(
            state = AuthenticationSettingsState.AccessDenied,
            savedSettings = null,
            draftSettings = null,
            pendingConfirmation = null,
          )
        }
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
          apiState = AuthenticationSettingsApiState.Success(AuthenticationSettingsOperation.Save)
        )
    }
  }

  suspend fun saveSettings(uiState: AuthenticationSettingsUiState): AuthenticationSettingsUiState =
    save(uiState)
}

private fun Throwable.toAuthenticationSettingsState(): AuthenticationSettingsState =
  if (this is HttpException && code() == 403) {
    AuthenticationSettingsState.AccessDenied
  } else {
    AuthenticationSettingsState.Failure(message)
  }
