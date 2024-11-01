package dev.halim.shelfdroid.network

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class BatchLibraryItemsResponse(
    @SerialName("libraryItems")
    val libraryItems: List<LibraryItem> = listOf(),
)

@Serializable
data class BatchLibraryItemsRequest(val libraryItemIds: List<String>)
