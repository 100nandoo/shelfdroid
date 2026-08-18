package dev.halim.core.network.request.authenticationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Partial update for Authentication settings. Null fields are omitted by the shared serializer. */
@Serializable
data class UpdateAuthenticationSettingsRequest(
  @SerialName("authLoginCustomMessage") val authLoginCustomMessage: String? = null,
  @SerialName("authActiveAuthMethods") val authActiveAuthMethods: List<String>? = null,
  @SerialName("authOpenIDIssuerURL") val authOpenIDIssuerURL: String? = null,
  @SerialName("authOpenIDAuthorizationURL") val authOpenIDAuthorizationURL: String? = null,
  @SerialName("authOpenIDTokenURL") val authOpenIDTokenURL: String? = null,
  @SerialName("authOpenIDUserInfoURL") val authOpenIDUserInfoURL: String? = null,
  @SerialName("authOpenIDJwksURL") val authOpenIDJwksURL: String? = null,
  @SerialName("authOpenIDLogoutURL") val authOpenIDLogoutURL: String? = null,
  @SerialName("authOpenIDClientID") val authOpenIDClientID: String? = null,
  @SerialName("authOpenIDClientSecret") val authOpenIDClientSecret: String? = null,
  @SerialName("authOpenIDTokenSigningAlgorithm")
  val authOpenIDTokenSigningAlgorithm: String? = null,
  @SerialName("authOpenIDMobileRedirectURIs")
  val authOpenIDMobileRedirectURIs: List<String>? = null,
  @SerialName("authOpenIDSubfolderForRedirectURLs")
  val authOpenIDSubfolderForRedirectURLs: String? = null,
  @SerialName("authOpenIDButtonText") val authOpenIDButtonText: String? = null,
  @SerialName("authOpenIDMatchExistingBy") val authOpenIDMatchExistingBy: String? = null,
  @SerialName("authOpenIDAutoLaunch") val authOpenIDAutoLaunch: Boolean? = null,
  @SerialName("authOpenIDAutoRegister") val authOpenIDAutoRegister: Boolean? = null,
  @SerialName("authOpenIDGroupClaim") val authOpenIDGroupClaim: String? = null,
  @SerialName("authOpenIDAdvancedPermsClaim") val authOpenIDAdvancedPermsClaim: String? = null,
)
