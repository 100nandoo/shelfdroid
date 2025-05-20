package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable data class BatchLibraryItemsRequest(val libraryItemIds: List<String>)
