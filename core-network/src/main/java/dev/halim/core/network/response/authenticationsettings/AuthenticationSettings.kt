package dev.halim.core.network.response.authenticationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationSettingsResponse(
  @SerialName("authLoginCustomMessage") val authLoginCustomMessage: String? = null,
  @SerialName("authActiveAuthMethods") val authActiveAuthMethods: List<String> = emptyList(),
  @SerialName("authOpenIDIssuerURL") val authOpenIDIssuerURL: String? = null,
  @SerialName("authOpenIDAuthorizationURL") val authOpenIDAuthorizationURL: String? = null,
  @SerialName("authOpenIDTokenURL") val authOpenIDTokenURL: String? = null,
  @SerialName("authOpenIDUserInfoURL") val authOpenIDUserInfoURL: String? = null,
  @SerialName("authOpenIDJwksURL") val authOpenIDJwksURL: String? = null,
  @SerialName("authOpenIDLogoutURL") val authOpenIDLogoutURL: String? = null,
  @SerialName("authOpenIDClientID") val authOpenIDClientID: String? = null,
  @SerialName("authOpenIDClientSecret") val authOpenIDClientSecret: String? = null,
  @SerialName("authOpenIDTokenSigningAlgorithm") val authOpenIDTokenSigningAlgorithm: String? = null,
  @SerialName("authOpenIDMobileRedirectURIs") val authOpenIDMobileRedirectURIs: List<String> = emptyList(),
  @SerialName("authOpenIDSubfolderForRedirectURLs")
  val authOpenIDSubfolderForRedirectURLs: String = "",
  @SerialName("authOpenIDButtonText") val authOpenIDButtonText: String? = null,
  @SerialName("authOpenIDMatchExistingBy") val authOpenIDMatchExistingBy: String? = null,
  @SerialName("authOpenIDAutoLaunch") val authOpenIDAutoLaunch: Boolean = false,
  @SerialName("authOpenIDAutoRegister") val authOpenIDAutoRegister: Boolean = false,
  @SerialName("authOpenIDGroupClaim") val authOpenIDGroupClaim: String? = null,
  @SerialName("authOpenIDAdvancedPermsClaim") val authOpenIDAdvancedPermsClaim: String? = null,
  @SerialName("authOpenIDSamplePermissions") val authOpenIDSamplePermissions: String = "",
)

@Serializable
data class UpdateAuthenticationSettingsResponse(
  @SerialName("updated") val updated: Boolean = false,
)

/** OpenID Connect metadata returned by Audiobookshelf's authenticated discovery route. */
@Serializable
data class OpenIdIssuerConfigurationResponse(
  @SerialName("issuer") val issuer: String? = null,
  @SerialName("authorization_endpoint") val authorizationEndpoint: String? = null,
  @SerialName("token_endpoint") val tokenEndpoint: String? = null,
  @SerialName("userinfo_endpoint") val userInfoEndpoint: String? = null,
  @SerialName("end_session_endpoint") val endSessionEndpoint: String? = null,
  @SerialName("jwks_uri") val jwksUri: String? = null,
  @SerialName("id_token_signing_alg_values_supported")
  val signingAlgorithms: List<String> = emptyList(),
)
