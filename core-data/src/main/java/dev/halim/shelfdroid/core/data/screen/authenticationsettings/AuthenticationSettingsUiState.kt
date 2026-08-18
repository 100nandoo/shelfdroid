package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

data class AuthenticationSettingsUiState(
  val state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
  val savedSettings: AuthenticationSettingsForm? = null,
  val draftSettings: AuthenticationSettingsForm? = null,
  /** Algorithms offered by the most recent successful issuer discovery. */
  val signingAlgorithmOptions: List<String> = emptyList(),
  /** The only server-provided callback subfolder choices, including no subfolder. */
  val callbackSubfolderOptions: List<String> = listOf(""),
  val serverBaseUrl: String = AudiobookshelfBaseUrl.DEFAULT_VALUE,
  val apiState: AuthenticationSettingsApiState = AuthenticationSettingsApiState.Idle,
  val validation: AuthenticationSettingsValidation = AuthenticationSettingsValidation(),
  /** True while the ViewModel holds an explicit client-secret replacement or clear intent. */
  val clientSecretChangePending: Boolean = false,
  val pendingConfirmation: AuthenticationSettingsConfirmation? = null,
  val leaveRequested: Boolean = false,
  /** Remains visible after an accepted OIDC update until the screen is left or reset. */
  val restartRequired: Boolean = false,
)

val AuthenticationSettingsUiState.hasChanges: Boolean
  get() =
    clientSecretChangePending ||
      (savedSettings != null && draftSettings != null && savedSettings != draftSettings)

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

/** A secret update is never inferred from the redacted loaded settings. */
sealed interface AuthenticationSettingsSecretUpdate {
  data object Untouched : AuthenticationSettingsSecretUpdate

  data class Replace(val value: String) : AuthenticationSettingsSecretUpdate

  data object Clear : AuthenticationSettingsSecretUpdate
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
  InvalidExistingUserMatching,
  InvalidGroupClaim,
  InvalidAdvancedPermissionsClaim,
  InvalidMobileRedirectUri,
  WildcardMobileRedirectUriMustBeSoleEntry,
  InvalidCallbackSubfolder,
}

data class AuthenticationSettingsValidation(
  val errors: Set<AuthenticationSettingsValidationError> = emptySet(),
) {
  val isValid: Boolean
    get() = errors.isEmpty()
}

enum class AuthenticationSettingsConfirmation {
  DisablePasswordSignIn,
  ClearClientSecret,
  RemoveShelfDroidCallback,
  UseWildcardMobileRedirect,
  LeaveWithUnsavedChanges,
}

fun AuthenticationSettingsSummary.validation(
  secretUpdate: AuthenticationSettingsSecretUpdate = AuthenticationSettingsSecretUpdate.Untouched,
  callbackSubfolderOptions: Collection<String> = listOf(""),
): AuthenticationSettingsValidation {
  val errors = buildSet {
    if (activeLoginMethods.isEmpty()) {
      add(AuthenticationSettingsValidationError.NoLoginMethod)
    }
    if (
      LoginMethod.OpenId in activeLoginMethods &&
        !openId.isStructurallyValid(secretUpdate)
    ) {
      add(AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete)
    }
    if (openId.matchExistingBy !in OPENID_MATCH_EXISTING_BY_OPTIONS) {
      add(AuthenticationSettingsValidationError.InvalidExistingUserMatching)
    }
    if (!openId.groupClaim.isValidOpenIdClaim()) {
      add(AuthenticationSettingsValidationError.InvalidGroupClaim)
    }
    if (!openId.advancedPermsClaim.isValidOpenIdClaim()) {
      add(AuthenticationSettingsValidationError.InvalidAdvancedPermissionsClaim)
    }
    val redirectUris = openId.mobileRedirectUris
    if ("*" in redirectUris && redirectUris.size > 1) {
      add(AuthenticationSettingsValidationError.WildcardMobileRedirectUriMustBeSoleEntry)
    } else if (
      redirectUris.any {
        it != "*" && !it.matches(AUDIOBOOKSHELF_MOBILE_REDIRECT_URI_PATTERN)
      }
    ) {
      add(AuthenticationSettingsValidationError.InvalidMobileRedirectUri)
    }
    if (openId.subfolderForRedirectUrls !in callbackSubfolderOptions) {
      add(AuthenticationSettingsValidationError.InvalidCallbackSubfolder)
    }
  }
  return AuthenticationSettingsValidation(errors)
}

fun AuthenticationSettingsSummary.isOpenIdConfigurationValid(
  secretUpdate: AuthenticationSettingsSecretUpdate = AuthenticationSettingsSecretUpdate.Untouched,
): Boolean = openId.isStructurallyValid(secretUpdate)

private fun OpenIdSettingsSummary.isStructurallyValid(
  secretUpdate: AuthenticationSettingsSecretUpdate,
): Boolean =
  issuerUrl.isNotBlank() &&
    authorizationUrl.isNotBlank() &&
    tokenUrl.isNotBlank() &&
    userInfoUrl.isNotBlank() &&
    jwksUrl.isNotBlank() &&
    clientId.isNotBlank() &&
    tokenSigningAlgorithm.isNotBlank() &&
    secretUpdate.isAllowedFor(clientSecretConfigured)

private fun AuthenticationSettingsSecretUpdate.isAllowedFor(
  clientSecretConfigured: Boolean,
): Boolean =
  when (this) {
    AuthenticationSettingsSecretUpdate.Untouched -> true
    is AuthenticationSettingsSecretUpdate.Replace -> value.isNotBlank()
    AuthenticationSettingsSecretUpdate.Clear -> !clientSecretConfigured
  }

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

/** Values supported by Audiobookshelf for matching an existing User during OpenID login. */
val OPENID_MATCH_EXISTING_BY_OPTIONS: List<String> = listOf("", "email", "username")

private val OPENID_CLAIM_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

/** Mirrors the Audiobookshelf web client's optional claim-name validation. */
private fun String.isValidOpenIdClaim(): Boolean = isEmpty() || matches(OPENID_CLAIM_PATTERN)

data class OpenIdDiscoveryResult(
  val issuerUrl: String? = null,
  val authorizationUrl: String? = null,
  val tokenUrl: String? = null,
  val userInfoUrl: String? = null,
  val jwksUrl: String? = null,
  val logoutUrl: String? = null,
  val signingAlgorithms: List<String> = emptyList(),
)

/** The two callback endpoints an administrator must register with the identity provider. */
data class OpenIdCallbackUrls(
  val web: String,
  val mobile: String,
)

fun OpenIdSettingsSummary.callbackUrls(serverBaseUrl: String): OpenIdCallbackUrls {
  val base = AudiobookshelfBaseUrl.parse(serverBaseUrl) ?: AudiobookshelfBaseUrl.DEFAULT
  val subfolder = subfolderForRedirectUrls.trim().trimEnd('/')
  val prefix = if (subfolder.isEmpty()) "" else "/$subfolder".replace("//", "/")
  return OpenIdCallbackUrls(
    web = "${base.origin}$prefix/auth/openid/callback",
    mobile = "${base.origin}$prefix/auth/openid/mobile-redirect",
  )
}

fun callbackSubfolderOptions(serverBaseUrl: String = DataStoreManager.BASE_URL): List<String> {
  val basePath =
    (AudiobookshelfBaseUrl.parse(serverBaseUrl) ?: AudiobookshelfBaseUrl.DEFAULT).pathPrefix
  return listOf("", basePath).distinct()
}

/**
 * Mirrors Audiobookshelf's `isValidRedirectURI` in
 * `server/controllers/MiscController.js` (case-insensitive). Keep `*` outside this matcher: the
 * server treats it as a separate sole-entry wildcard.
 */
internal val AUDIOBOOKSHELF_MOBILE_REDIRECT_URI_PATTERN = Regex(
  "^\\w+://[\\w\\.-]+(/[\\w\\./-]*)*$",
  RegexOption.IGNORE_CASE,
)
