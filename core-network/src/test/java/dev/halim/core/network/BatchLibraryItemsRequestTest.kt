package dev.halim.core.network

import dev.halim.core.network.request.BatchLibraryItemsRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class BatchLibraryItemsRequestTest {

  @Test
  fun acceptsExactly50Ids() {
    val request = BatchLibraryItemsRequest(List(50) { "item-$it" })

    assertEquals(50, request.libraryItemIds.size)
  }

  @Test
  fun rejectsMoreThan50Ids() {
    assertThrows(IllegalArgumentException::class.java) {
      BatchLibraryItemsRequest(List(51) { "item-$it" })
    }
  }
}
