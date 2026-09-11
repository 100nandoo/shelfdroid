package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLibraryItemChaptersResponse(
  @SerialName("success") val success: Boolean = false,
  @SerialName("updated") val updated: Boolean = false,
)
