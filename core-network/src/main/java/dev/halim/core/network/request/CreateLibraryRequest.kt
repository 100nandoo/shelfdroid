package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Payload accepted by Audiobookshelf's POST /api/libraries endpoint. */
@Serializable
data class CreateLibraryRequest(
  @SerialName("name") val name: String,
  @SerialName("folders") val folders: List<Folder>,
  @SerialName("mediaType") val mediaType: String,
  @SerialName("icon") val icon: String,
  @SerialName("provider") val provider: String,
) {
  @Serializable
  data class Folder(@SerialName("path") val path: String)
}
