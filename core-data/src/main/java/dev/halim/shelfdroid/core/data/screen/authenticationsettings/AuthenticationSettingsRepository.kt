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
        AuthenticationSettingsUiState(
          state = AuthenticationSettingsState.Ready(AuthenticationSettingsMapper.map(response))
        )
      },
      onFailure = { error ->
        AuthenticationSettingsUiState(error.toAuthenticationSettingsState())
      },
    )
  }
}

private fun Throwable.toAuthenticationSettingsState(): AuthenticationSettingsState =
  if (this is HttpException && code() == 403) {
    AuthenticationSettingsState.AccessDenied
  } else {
    AuthenticationSettingsState.Failure(message)
  }
