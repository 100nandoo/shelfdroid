package dev.halim.shelfdroid.core.ui.screen.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiscScreenTest {

  @Test
  fun serverSection_isVisibleOnlyForAdmins() {
    assertTrue(shouldShowServerSection(isAdmin = true))
    assertFalse(shouldShowServerSection(isAdmin = false))
  }

}
