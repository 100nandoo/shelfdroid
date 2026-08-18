package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.request.authenticationsettings.UpdateAuthenticationSettingsRequest
import dev.halim.core.network.response.authenticationsettings.AuthenticationSettingsResponse
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

object AuthenticationSettingsMapper {

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
  ): UpdateAuthenticationSettingsRequest? {
    val customMessage =
      draft.customMessageValue().takeIf { it != saved.customMessageValue() }
    val methods =
      draft.activeLoginMethods.normalizedMethods().takeIf {
        it != saved.activeLoginMethods.normalizedMethods()
      }
    if (customMessage == null && methods == null) return null
    return UpdateAuthenticationSettingsRequest(
      authLoginCustomMessage = customMessage,
      authActiveAuthMethods = methods?.map { it.toWireValue() },
    )
  }

  private fun String.toLoginMethodOrNull(): LoginMethod? =
    when (trim().lowercase()) {
      "local" -> LoginMethod.Local
      "openid" -> LoginMethod.OpenId
      else -> null
    }

  private fun List<LoginMethod>.normalizedMethods(): List<LoginMethod> =
    buildList {
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
