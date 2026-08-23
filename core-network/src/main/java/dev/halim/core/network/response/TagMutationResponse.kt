package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TagMutationResponse(
  @SerialName("numItemsUpdated") val numItemsUpdated: Int = 0,
  @SerialName("tagMerged") val tagMerged: Boolean = false,
)
