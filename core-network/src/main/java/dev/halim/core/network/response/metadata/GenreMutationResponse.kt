package dev.halim.core.network.response.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenreMutationResponse(
  @SerialName("numItemsUpdated") val numItemsUpdated: Int = 0,
  @SerialName("genreMerged") val genreMerged: Boolean = false,
)
