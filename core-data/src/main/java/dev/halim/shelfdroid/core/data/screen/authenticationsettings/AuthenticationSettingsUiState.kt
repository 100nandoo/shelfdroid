package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

data class AuthenticationSettingsUiState(
  val state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
)

sealed interface AuthenticationSettingsState {
  data object Loading : AuthenticationSettingsState

  data class Ready(val settings: AuthenticationSettingsSummary) : AuthenticationSettingsState

  data object AccessDenied : AuthenticationSettingsState

  data class Failure(val message: String?) : AuthenticationSettingsState
}

data class AuthenticationSettingsSummary(
  val customMessageEnabled: Boolean = false,
  val activeLoginMethods: List<LoginMethod> = emptyList(),
  val openId: OpenIdSettingsSummary = OpenIdSettingsSummary(),
)

data class OpenIdSettingsSummary(
  val issuerUrl: String = "",
  val authorizationUrl: String = "",
  val tokenUrl: String = "",
  val userInfoUrl: String = "",
  val jwksUrl: String = "",
  val logoutUrl: String = "",
  val clientId: String = "",
  val clientSecretConfigured: Boolean = false,
  val tokenSigningAlgorithm: String = "",
  val mobileRedirectUris: List<String> = emptyList(),
  val subfolderForRedirectUrls: String = "",
  val buttonText: String = "",
  val matchExistingBy: String = "",
  val autoLaunch: Boolean = false,
  val autoRegister: Boolean = false,
  val groupClaim: String = "",
  val advancedPermsClaim: String = "",
  val samplePermissions: String = "",
)
