package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Result returned after Audiobookshelf updates matching Library items for a Genre. */
@Serializable
data class GenreMutationResponse(
  @SerialName("numItemsUpdated") val numItemsUpdated: Int = 0,
  @SerialName("genreMerged") val genreMerged: Boolean = false,
)
