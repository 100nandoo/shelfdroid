package dev.halim.core.network.request.metadata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RenameGenreRequest(
  @SerialName("genre") val genre: String,
  @SerialName("newGenre") val newGenre: String,
)
