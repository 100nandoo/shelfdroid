package dev.halim.core.network.request.authenticationsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Partial update for the ticket-02 login message and method settings. */
@Serializable
data class UpdateAuthenticationSettingsRequest(
  @SerialName("authLoginCustomMessage") val authLoginCustomMessage: String? = null,
  @SerialName("authActiveAuthMethods") val authActiveAuthMethods: List<String>? = null,
)
