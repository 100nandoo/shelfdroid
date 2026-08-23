package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RenameTagRequest(
  @SerialName("tag") val tag: String,
  @SerialName("newTag") val newTag: String,
)
