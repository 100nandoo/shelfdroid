package dev.halim.core.network.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class UpdateLibraryRequest(
  @SerialName("name") val name: String? = null,
  @SerialName("folders") val folders: List<Folder>? = null,
  @SerialName("icon") val icon: String? = null,
  @SerialName("provider") val provider: String? = null,
  @SerialName("settings") val settings: JsonObject? = null,
) {
  @Serializable
  data class Folder(
    @SerialName("id") val id: String? = null,
    @SerialName("path") val path: String,
  )
}
