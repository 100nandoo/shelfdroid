package dev.halim.shelfdroid.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudiobookshelfBaseUrlTest {

  @Test
  fun parse_defaultsBareHostToHttps() {
    val baseUrl = checkNotNull(AudiobookshelfBaseUrl.parse("audiobooks.dev"))

    assertEquals("https://audiobooks.dev", baseUrl.value)
    assertEquals(
      "https://audiobooks.dev/api/items/item-1/cover",
      baseUrl.resolve("/api/items/item-1/cover"),
    )
  }

  @Test
  fun parse_preservesPortAndSubpathAndDropsQueryAndFragment() {
    val baseUrl =
      checkNotNull(
        AudiobookshelfBaseUrl.parse("https://example.com:13378/audiobookshelf/?x=1#demo")
      )

    assertEquals("https://example.com:13378/audiobookshelf", baseUrl.value)
    assertEquals(
      "https://example.com:13378/audiobookshelf/api/me?expanded=1",
      baseUrl.resolve("/api/me", query = "expanded=1"),
    )
    assertEquals("/audiobookshelf/socket.io", baseUrl.socketPath())
  }

  @Test
  fun parse_rejectsUnsupportedScheme() {
    assertNull(AudiobookshelfBaseUrl.parse("ftp://example.com"))
  }
}
