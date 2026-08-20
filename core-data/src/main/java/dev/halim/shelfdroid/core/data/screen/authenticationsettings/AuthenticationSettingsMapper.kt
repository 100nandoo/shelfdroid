package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.request.authenticationsettings.UpdateAuthenticationSettingsRequest
import dev.halim.core.network.response.authenticationsettings.AuthenticationSettingsResponse
import dev.halim.core.network.response.authenticationsettings.OpenIdIssuerConfigurationResponse
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

object AuthenticationSettingsMapper {

  fun mapDiscovery(response: OpenIdIssuerConfigurationResponse): OpenIdDiscoveryResult =
    OpenIdDiscoveryResult(
      issuerUrl = response.issuer,
      authorizationUrl = response.authorizationEndpoint,
      tokenUrl = response.tokenEndpoint,
      userInfoUrl = response.userInfoEndpoint,
      jwksUrl = response.jwksUri,
      logoutUrl = response.endSessionEndpoint,
      signingAlgorithms = response.signingAlgorithms.distinct().filter(String::isNotBlank),
    )

  fun map(response: AuthenticationSettingsResponse): AuthenticationSettingsSummary =
    AuthenticationSettingsSummary(
      customMessageEnabled = !response.authLoginCustomMessage.isNullOrEmpty(),
      customMessage = response.authLoginCustomMessage.orEmpty(),
      activeLoginMethods = response.authActiveAuthMethods.mapNotNull { it.toLoginMethodOrNull() },
      openId =
        OpenIdSettingsSummary(
          issuerUrl = response.authOpenIDIssuerURL.orEmpty(),
          authorizationUrl = response.authOpenIDAuthorizationURL.orEmpty(),
          tokenUrl = response.authOpenIDTokenURL.orEmpty(),
          userInfoUrl = response.authOpenIDUserInfoURL.orEmpty(),
          jwksUrl = response.authOpenIDJwksURL.orEmpty(),
          logoutUrl = response.authOpenIDLogoutURL.orEmpty(),
          clientId = response.authOpenIDClientID.orEmpty(),
          clientSecretConfigured = !response.authOpenIDClientSecret.isNullOrEmpty(),
          tokenSigningAlgorithm = response.authOpenIDTokenSigningAlgorithm.orEmpty(),
          mobileRedirectUris = response.authOpenIDMobileRedirectURIs,
          subfolderForRedirectUrls = response.authOpenIDSubfolderForRedirectURLs,
          buttonText = response.authOpenIDButtonText.orEmpty(),
          matchExistingBy = response.authOpenIDMatchExistingBy.orEmpty(),
          autoLaunch = response.authOpenIDAutoLaunch,
          autoRegister = response.authOpenIDAutoRegister,
          groupClaim = response.authOpenIDGroupClaim.orEmpty(),
          advancedPermsClaim = response.authOpenIDAdvancedPermsClaim.orEmpty(),
          samplePermissions = response.authOpenIDSamplePermissions,
        ),
    )

  fun toUpdateRequest(
    saved: AuthenticationSettingsForm,
    draft: AuthenticationSettingsForm,
    secretUpdate: AuthenticationSettingsSecretUpdate = AuthenticationSettingsSecretUpdate.Untouched,
  ): UpdateAuthenticationSettingsRequest? {
    val customMessage = draft.customMessageValue().takeIf { it != saved.customMessageValue() }
    val methods =
      draft.activeLoginMethods.normalizedMethods().takeIf {
        it != saved.activeLoginMethods.normalizedMethods()
      }
    val savedOpenId = saved.openId
    val draftOpenId = draft.openId
    val clientSecret = secretUpdate.toRequestValue()
    if (
      customMessage == null &&
        methods == null &&
        !hasOpenIdProviderChanges(savedOpenId, draftOpenId) &&
        clientSecret == null
    ) {
      return null
    }
    return UpdateAuthenticationSettingsRequest(
      authLoginCustomMessage = customMessage,
      authActiveAuthMethods = methods?.map { it.toWireValue() },
      authOpenIDIssuerURL = draftOpenId.issuerUrl.takeIf { it != savedOpenId.issuerUrl },
      authOpenIDAuthorizationURL =
        draftOpenId.authorizationUrl.takeIf { it != savedOpenId.authorizationUrl },
      authOpenIDTokenURL = draftOpenId.tokenUrl.takeIf { it != savedOpenId.tokenUrl },
      authOpenIDUserInfoURL = draftOpenId.userInfoUrl.takeIf { it != savedOpenId.userInfoUrl },
      authOpenIDJwksURL = draftOpenId.jwksUrl.takeIf { it != savedOpenId.jwksUrl },
      authOpenIDLogoutURL = draftOpenId.logoutUrl.takeIf { it != savedOpenId.logoutUrl },
      authOpenIDClientID = draftOpenId.clientId.takeIf { it != savedOpenId.clientId },
      authOpenIDClientSecret = clientSecret,
      authOpenIDTokenSigningAlgorithm =
        draftOpenId.tokenSigningAlgorithm.takeIf {
          it != savedOpenId.tokenSigningAlgorithm
        },
      authOpenIDMobileRedirectURIs =
        draftOpenId.mobileRedirectUris.takeIf {
          it != savedOpenId.mobileRedirectUris
        },
      authOpenIDSubfolderForRedirectURLs =
        draftOpenId.subfolderForRedirectUrls.takeIf {
          it != savedOpenId.subfolderForRedirectUrls
        },
      authOpenIDButtonText = draftOpenId.buttonText.takeIf { it != savedOpenId.buttonText },
      authOpenIDMatchExistingBy =
        draftOpenId.matchExistingBy.normalizedClaimValue().takeIf {
          it != savedOpenId.matchExistingBy
        },
      authOpenIDAutoLaunch = draftOpenId.autoLaunch.takeIf { it != savedOpenId.autoLaunch },
      authOpenIDAutoRegister = draftOpenId.autoRegister.takeIf { it != savedOpenId.autoRegister },
      authOpenIDGroupClaim =
        draftOpenId.groupClaim.normalizedClaimValue().takeIf {
          it != savedOpenId.groupClaim
        },
      authOpenIDAdvancedPermsClaim =
        draftOpenId.advancedPermsClaim.normalizedClaimValue().takeIf {
          it != savedOpenId.advancedPermsClaim
        },
    )
  }

  fun hasOpenIdChanges(
    saved: AuthenticationSettingsForm,
    draft: AuthenticationSettingsForm,
    secretUpdate: AuthenticationSettingsSecretUpdate = AuthenticationSettingsSecretUpdate.Untouched,
  ): Boolean =
    hasOpenIdProviderChanges(saved.openId, draft.openId) ||
      secretUpdate != AuthenticationSettingsSecretUpdate.Untouched

  private fun AuthenticationSettingsSecretUpdate.toRequestValue(): String? =
    when (this) {
      AuthenticationSettingsSecretUpdate.Untouched -> null
      is AuthenticationSettingsSecretUpdate.Replace -> value
      AuthenticationSettingsSecretUpdate.Clear -> ""
    }

  private fun hasOpenIdProviderChanges(
    saved: OpenIdSettingsSummary,
    draft: OpenIdSettingsSummary,
  ): Boolean =
    saved.issuerUrl != draft.issuerUrl ||
      saved.authorizationUrl != draft.authorizationUrl ||
      saved.tokenUrl != draft.tokenUrl ||
      saved.userInfoUrl != draft.userInfoUrl ||
      saved.jwksUrl != draft.jwksUrl ||
      saved.logoutUrl != draft.logoutUrl ||
      saved.clientId != draft.clientId ||
      saved.tokenSigningAlgorithm != draft.tokenSigningAlgorithm ||
      saved.mobileRedirectUris != draft.mobileRedirectUris ||
      saved.subfolderForRedirectUrls != draft.subfolderForRedirectUrls ||
      saved.buttonText != draft.buttonText ||
      saved.matchExistingBy != draft.matchExistingBy ||
      saved.autoLaunch != draft.autoLaunch ||
      saved.autoRegister != draft.autoRegister ||
      saved.groupClaim != draft.groupClaim ||
      saved.advancedPermsClaim != draft.advancedPermsClaim

  private fun String.normalizedClaimValue(): String = if (isBlank()) "" else this

  fun mergeDiscovery(
    current: AuthenticationSettingsSummary,
    operationStart: AuthenticationSettingsSummary,
    discovery: OpenIdDiscoveryResult,
  ): AuthenticationSettingsSummary {
    val draft = current.openId
    val started = operationStart.openId
    val discovered =
      draft.copy(
        issuerUrl = discoveredValue(draft.issuerUrl, started.issuerUrl, discovery.issuerUrl),
        authorizationUrl =
          discoveredValue(
            draft.authorizationUrl,
            started.authorizationUrl,
            discovery.authorizationUrl,
          ),
        tokenUrl = discoveredValue(draft.tokenUrl, started.tokenUrl, discovery.tokenUrl),
        userInfoUrl =
          discoveredValue(draft.userInfoUrl, started.userInfoUrl, discovery.userInfoUrl),
        jwksUrl = discoveredValue(draft.jwksUrl, started.jwksUrl, discovery.jwksUrl),
        logoutUrl = discoveredValue(draft.logoutUrl, started.logoutUrl, discovery.logoutUrl),
        tokenSigningAlgorithm =
          if (
            draft.tokenSigningAlgorithm == started.tokenSigningAlgorithm &&
              discovery.signingAlgorithms.isNotEmpty() &&
              draft.tokenSigningAlgorithm !in discovery.signingAlgorithms
          ) {
            discovery.signingAlgorithms.first()
          } else {
            draft.tokenSigningAlgorithm
          },
      )
    return current.copy(openId = discovered)
  }

  private fun discoveredValue(current: String, started: String, discovered: String?): String =
    if (current == started) discovered ?: current else current

  private fun String.toLoginMethodOrNull(): LoginMethod? =
    when (trim().lowercase()) {
      "local" -> LoginMethod.Local
      "openid" -> LoginMethod.OpenId
      else -> null
    }

  private fun List<LoginMethod>.normalizedMethods(): List<LoginMethod> = buildList {
    if (LoginMethod.Local in this@normalizedMethods) add(LoginMethod.Local)
    if (LoginMethod.OpenId in this@normalizedMethods) add(LoginMethod.OpenId)
  }

  private fun LoginMethod.toWireValue(): String =
    when (this) {
      LoginMethod.Local -> "local"
      LoginMethod.OpenId -> "openid"
    }

  private fun AuthenticationSettingsForm.customMessageValue(): String =
    if (customMessageEnabled) customMessage else ""
}
