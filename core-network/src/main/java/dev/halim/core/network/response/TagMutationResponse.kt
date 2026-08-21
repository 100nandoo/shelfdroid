package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Result returned after Audiobookshelf updates matching Library items. */
@Serializable
data class TagMutationResponse(
  @SerialName("numItemsUpdated") val numItemsUpdated: Int = 0,
  @SerialName("tagMerged") val tagMerged: Boolean = false,
)
