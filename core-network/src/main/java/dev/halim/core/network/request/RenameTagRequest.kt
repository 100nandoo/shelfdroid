package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Request used by Audiobookshelf's server-wide tag rename operation. */
@Serializable
data class RenameTagRequest(
  @SerialName("tag") val tag: String,
  @SerialName("newTag") val newTag: String,
)
