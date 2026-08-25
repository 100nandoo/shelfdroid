package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One entry in the array accepted by Audiobookshelf's library reorder endpoint. */
@Serializable
data class ReorderLibraryRequest(
  @SerialName("id") val id: String,
  @SerialName("newOrder") val newOrder: Int,
)
