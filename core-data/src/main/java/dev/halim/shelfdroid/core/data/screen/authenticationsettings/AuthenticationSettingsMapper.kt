package dev.halim.shelfdroid.core.data.screen.authenticationsettings

import dev.halim.core.network.response.authenticationsettings.AuthenticationSettingsResponse
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod

object AuthenticationSettingsMapper {

  fun map(response: AuthenticationSettingsResponse): AuthenticationSettingsSummary =
    AuthenticationSettingsSummary(
      customMessageEnabled = !response.authLoginCustomMessage.isNullOrEmpty(),
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

  private fun String.toLoginMethodOrNull(): LoginMethod? =
    when (trim().lowercase()) {
      "local" -> LoginMethod.Local
      "openid" -> LoginMethod.OpenId
      else -> null
    }
}
