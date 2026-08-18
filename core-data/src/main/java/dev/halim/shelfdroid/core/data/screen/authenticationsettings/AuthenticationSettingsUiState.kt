package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

data class AuthenticationSettingsUiState(
  val state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
  val savedSettings: AuthenticationSettingsForm? = null,
  val draftSettings: AuthenticationSettingsForm? = null,
  /** Algorithms offered by the most recent successful issuer discovery. */
  val signingAlgorithmOptions: List<String> = emptyList(),
  val apiState: AuthenticationSettingsApiState = AuthenticationSettingsApiState.Idle,
  val validation: AuthenticationSettingsValidation = AuthenticationSettingsValidation(),
  val pendingConfirmation: AuthenticationSettingsConfirmation? = null,
  val leaveRequested: Boolean = false,
  /** Remains visible after an accepted OIDC update until the screen is left or reset. */
  val restartRequired: Boolean = false,
)

val AuthenticationSettingsUiState.hasChanges: Boolean
  get() = savedSettings != null && draftSettings != null && savedSettings != draftSettings

val AuthenticationSettingsUiState.canSave: Boolean
  get() = hasChanges && validation.isValid && apiState !is AuthenticationSettingsApiState.Loading

typealias AuthenticationSettingsForm = AuthenticationSettingsSummary

sealed interface AuthenticationSettingsState {
  data object Loading : AuthenticationSettingsState

  data class Ready(val settings: AuthenticationSettingsSummary) : AuthenticationSettingsState

  data object AccessDenied : AuthenticationSettingsState

  data class Failure(val message: String?) : AuthenticationSettingsState
}

data class AuthenticationSettingsSummary(
  val customMessageEnabled: Boolean = false,
  val customMessage: String = "",
  val activeLoginMethods: List<LoginMethod> = emptyList(),
  val openId: OpenIdSettingsSummary = OpenIdSettingsSummary(),
)

enum class AuthenticationSettingsOperation {
  Load,
  Discovery,
  Save,
}

sealed interface AuthenticationSettingsApiState {
  data object Idle : AuthenticationSettingsApiState

  data class Loading(val operation: AuthenticationSettingsOperation) :
    AuthenticationSettingsApiState

  data class Success(val operation: AuthenticationSettingsOperation) :
    AuthenticationSettingsApiState

  data class Failure(
    val operation: AuthenticationSettingsOperation,
    val message: String?,
  ) : AuthenticationSettingsApiState

  data object Rejected : AuthenticationSettingsApiState
}

enum class AuthenticationSettingsValidationError {
  NoLoginMethod,
  OpenIdConfigurationIncomplete,
}

data class AuthenticationSettingsValidation(
  val errors: Set<AuthenticationSettingsValidationError> = emptySet(),
) {
  val isValid: Boolean
    get() = errors.isEmpty()
}

enum class AuthenticationSettingsConfirmation {
  DisablePasswordSignIn,
  LeaveWithUnsavedChanges,
}

fun AuthenticationSettingsSummary.validation(): AuthenticationSettingsValidation {
  val errors = buildSet {
    if (activeLoginMethods.isEmpty()) {
      add(AuthenticationSettingsValidationError.NoLoginMethod)
    }
    if (
      LoginMethod.OpenId in activeLoginMethods &&
        !openId.hasStructurallyValidConfiguration()
    ) {
      add(AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete)
    }
  }
  return AuthenticationSettingsValidation(errors)
}

fun AuthenticationSettingsSummary.isOpenIdConfigurationValid(): Boolean =
  openId.hasStructurallyValidConfiguration()

private fun OpenIdSettingsSummary.hasStructurallyValidConfiguration(): Boolean =
  issuerUrl.isNotBlank() &&
    authorizationUrl.isNotBlank() &&
    tokenUrl.isNotBlank() &&
    userInfoUrl.isNotBlank() &&
    jwksUrl.isNotBlank() &&
    clientId.isNotBlank() &&
    tokenSigningAlgorithm.isNotBlank()

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

data class OpenIdDiscoveryResult(
  val issuerUrl: String? = null,
  val authorizationUrl: String? = null,
  val tokenUrl: String? = null,
  val userInfoUrl: String? = null,
  val jwksUrl: String? = null,
  val logoutUrl: String? = null,
  val signingAlgorithms: List<String> = emptyList(),
)
