package dev.halim.shelfdroid.media.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterSessionCommandAccessTest {
  private val access = ChapterSessionCommandAccess("dev.halim.shelfdroid")

  @Test
  fun `trusted same app controller is allowed`() {
    assertTrue(access.isAllowed("dev.halim.shelfdroid", isTrusted = true))
  }

  @Test
  fun `untrusted same app controller is rejected`() {
    assertFalse(access.isAllowed("dev.halim.shelfdroid", isTrusted = false))
  }

  @Test
  fun `trusted different app controller is rejected`() {
    assertFalse(access.isAllowed("com.example.controller", isTrusted = true))
  }
}
