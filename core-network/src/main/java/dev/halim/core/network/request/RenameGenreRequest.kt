package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request used by Audiobookshelf's server-wide genre rename operation. */
@Serializable
data class RenameGenreRequest(
  @SerialName("genre") val genre: String,
  @SerialName("newGenre") val newGenre: String,
)
