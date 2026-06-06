package dev.halim.shelfdroid.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadableStoragePolicyTest {
  private val policy = ReadableStoragePolicy()

  @Test
  fun `podcastRelativePath keeps readable folder tree under downloads`() {
    val relativePath = policy.podcastRelativePath("The Daily")

    assertTrue(relativePath.endsWith("/ShelfDroid/podcasts/The Daily/"))
  }

  @Test
  fun `sanitizePathSegment removes invalid path characters`() {
    val sanitized = policy.sanitizePathSegment("""  A/B:C*"D"?<E>|  """)

    assertEquals("A_B_C__D___E__", sanitized)
  }

  @Test
  fun `sanitizePathSegment falls back when segment becomes blank`() {
    val sanitized = policy.sanitizePathSegment("...   ")

    assertEquals("untitled", sanitized)
  }
}
