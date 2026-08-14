package dev.halim.core.network.request

import kotlinx.serialization.Serializable

@Serializable
data class BatchLibraryItemsRequest(val libraryItemIds: List<String>) {
  init {
    require(libraryItemIds.size <= MAX_LIBRARY_ITEM_IDS) {
      "Batch library item requests may contain at most $MAX_LIBRARY_ITEM_IDS IDs"
    }
  }

  companion object {
    const val MAX_LIBRARY_ITEM_IDS = 50
  }
}
