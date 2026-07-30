package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginStatusResponse(
  @SerialName("app") val app: String = "",
  @SerialName("serverVersion") val serverVersion: String = "",
  @SerialName("isInit") val isInit: Boolean = false,
  @SerialName("language") val language: String = "",
  @SerialName("authMethods") val authMethods: List<String> = emptyList(),
  @SerialName("authFormData") val authFormData: LoginAuthFormData? = null,
)

@Serializable
data class LoginAuthFormData(
  @SerialName("authLoginCustomMessage") val authLoginCustomMessage: String? = null,
  @SerialName("authOpenIDButtonText") val authOpenIDButtonText: String? = null,
  @SerialName("authOpenIDAutoLaunch") val authOpenIDAutoLaunch: Boolean? = null,
)
