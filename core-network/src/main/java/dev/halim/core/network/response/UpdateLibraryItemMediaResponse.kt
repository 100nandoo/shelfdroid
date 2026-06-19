package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateLibraryItemMediaResponse(
  @SerialName("updated") val updated: String = "",
  @SerialName("libraryItem") val libraryItem: LibraryItem? = null,
)
