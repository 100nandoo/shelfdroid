package dev.halim.shelfdroid.core.ui.screen.home

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MiscScreenTest {

  @Test
  fun authenticationEntry_isVisibleOnlyForAdmins() {
    assertTrue(shouldShowAuthenticationSettings(isAdmin = true))
    assertFalse(shouldShowAuthenticationSettings(isAdmin = false))
  }
}
