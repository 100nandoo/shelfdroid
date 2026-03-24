package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateApiKeyRequest(
  @SerialName("name") val name: String,
  @SerialName("expiresIn") val expiresIn: Int,
  @SerialName("isActive") val isActive: Boolean,
  @SerialName("userId") val userId: String,
)
