package dev.halim.shelfdroid.core.data.metadata

import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataUtilitiesRepositoryTest {
  @Test
  fun encodeTagPath_usesUtf8StandardBase64ThenUriEscaping() {
    // "science/fiction + 日本" encodes to a path-safe value with no raw Base64 delimiters.
    val encoded = encodeTagPath("science/fiction + 日本")

    assertEquals("c2NpZW5jZS9maWN0aW9uICsg5pel5pys", encoded)
    org.junit.Assert.assertFalse(encoded.contains('/'))
    org.junit.Assert.assertFalse(encoded.contains('+'))

    assertEquals("AAA%2B", encodeTagPath("\u0000\u0000>"))
    assertEquals("AAA%2F", encodeTagPath("\u0000\u0000?"))
    assertEquals("Zg%3D%3D", encodeTagPath("f"))
  }
}
