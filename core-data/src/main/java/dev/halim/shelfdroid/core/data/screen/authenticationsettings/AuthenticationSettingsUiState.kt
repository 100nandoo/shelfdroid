package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.datastore.DataStoreManager

data class AuthenticationSettingsUiState(
  val state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
  val savedSettings: AuthenticationSettingsForm? = null,
  val draftSettings: AuthenticationSettingsForm? = null,
  val signingAlgorithmOptions: List<String> = emptyList(),
  val callbackSubfolderOptions: List<String> = listOf(""),
  val serverBaseUrl: String = AudiobookshelfBaseUrl.DEFAULT_VALUE,
  val apiState: AuthenticationSettingsApiState = AuthenticationSettingsApiState.Idle,
  val validation: AuthenticationSettingsValidation = AuthenticationSettingsValidation(),
  val pendingConfirmation: AuthenticationSettingsConfirmation? = null,
  val leaveRequested: Boolean = false,
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
  InvalidExistingUserMatching,
  InvalidGroupClaim,
  InvalidAdvancedPermissionsClaim,
  InvalidMobileRedirectUri,
  WildcardMobileRedirectUriMustBeSoleEntry,
  InvalidCallbackSubfolder,
}

data class AuthenticationSettingsValidation(
  val errors: Set<AuthenticationSettingsValidationError> = emptySet()
) {
  val isValid: Boolean
    get() = errors.isEmpty()
}

enum class AuthenticationSettingsConfirmation {
  DisablePasswordSignIn,
  RemoveShelfDroidCallback,
  UseWildcardMobileRedirect,
  LeaveWithUnsavedChanges,
}

fun AuthenticationSettingsSummary.validation(
  callbackSubfolderOptions: Collection<String> = listOf("")
): AuthenticationSettingsValidation {
  val errors = buildSet {
    if (activeLoginMethods.isEmpty()) {
      add(AuthenticationSettingsValidationError.NoLoginMethod)
    }
    if (LoginMethod.OpenId in activeLoginMethods && !openId.isStructurallyValid()) {
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

fun AuthenticationSettingsSummary.isOpenIdConfigurationValid(): Boolean =
  openId.isStructurallyValid()

private fun OpenIdSettingsSummary.isStructurallyValid(): Boolean =
  issuerUrl.isNotBlank() &&
    authorizationUrl.isNotBlank() &&
    tokenUrl.isNotBlank() &&
    userInfoUrl.isNotBlank() &&
    jwksUrl.isNotBlank() &&
    clientId.isNotBlank() &&
    tokenSigningAlgorithm.isNotBlank() &&
    clientSecret.isNotBlank()

data class OpenIdSettingsSummary(
  val issuerUrl: String = "",
  val authorizationUrl: String = "",
  val tokenUrl: String = "",
  val userInfoUrl: String = "",
  val jwksUrl: String = "",
  val logoutUrl: String = "",
  val clientId: String = "",
  val clientSecret: String = "",
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
) {
  override fun toString(): String =
    "OpenIdSettingsSummary(" +
      "issuerUrl=$issuerUrl, " +
      "authorizationUrl=$authorizationUrl, " +
      "tokenUrl=$tokenUrl, " +
      "userInfoUrl=$userInfoUrl, " +
      "jwksUrl=$jwksUrl, " +
      "logoutUrl=$logoutUrl, " +
      "clientId=$clientId, " +
      "clientSecret=<redacted>, " +
      "tokenSigningAlgorithm=$tokenSigningAlgorithm, " +
      "mobileRedirectUris=$mobileRedirectUris, " +
      "subfolderForRedirectUrls=$subfolderForRedirectUrls, " +
      "buttonText=$buttonText, " +
      "matchExistingBy=$matchExistingBy, " +
      "autoLaunch=$autoLaunch, " +
      "autoRegister=$autoRegister, " +
      "groupClaim=$groupClaim, " +
      "advancedPermsClaim=$advancedPermsClaim, " +
      "samplePermissions=$samplePermissions)"
}

val OPENID_MATCH_EXISTING_BY_OPTIONS: List<String> = listOf("", "email", "username")

private val OPENID_CLAIM_PATTERN = Regex("^[a-zA-Z][a-zA-Z0-9_-]*$")

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

internal val AUDIOBOOKSHELF_MOBILE_REDIRECT_URI_PATTERN =
  Regex(
    "^\\w+://[\\w\\.-]+(/[\\w\\./-]*)*$",
    RegexOption.IGNORE_CASE,
  )
