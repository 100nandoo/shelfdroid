package dev.halim.core.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BatchLibraryItemsResponse(
  @SerialName("libraryItems") val libraryItems: List<LibraryItem> = listOf()
)
