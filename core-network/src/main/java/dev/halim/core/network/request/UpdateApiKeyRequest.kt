package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateApiKeyRequest(
  @SerialName("isActive") val isActive: Boolean,
  @SerialName("userId") val userId: String,
)
