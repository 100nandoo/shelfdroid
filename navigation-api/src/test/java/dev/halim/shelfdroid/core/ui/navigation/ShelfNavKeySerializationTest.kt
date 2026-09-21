package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.navigation.NavEditApiKeys
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShelfNavKeySerializationTest {
  @Test
  fun plainKeyKeepsItsSerializedName() {
    val key: ShelfNavKey = Book("book-1")

    val encoded = Json.encodeToString(ShelfNavKey.serializer(), key)

    assertTrue(encoded.contains("dev.halim.shelfdroid.core.ui.navigation.Book"))
    assertEquals(key, Json.decodeFromString(ShelfNavKey.serializer(), encoded))
  }

  @Test
  fun corePayloadKeyRoundTrips() {
    val key: ShelfNavKey = EditApiKeys(NavEditApiKeys(id = "key-1", userId = "user-1"))

    val encoded = Json.encodeToString(ShelfNavKey.serializer(), key)

    assertTrue(encoded.contains("dev.halim.shelfdroid.core.ui.navigation.EditApiKeys"))
    assertEquals(key, Json.decodeFromString(ShelfNavKey.serializer(), encoded))
  }
}
