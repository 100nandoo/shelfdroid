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

  @Test
  fun metadataUtilitiesEntry_isVisibleOnlyForAdmins() {
    assertTrue(shouldShowLibraryItemMetadataUtilities(isAdmin = true))
    assertFalse(shouldShowLibraryItemMetadataUtilities(isAdmin = false))
  }

  @Test
  fun librariesEntry_isVisibleOnlyForAdmins() {
    assertTrue(shouldShowLibraryAdministration(isAdmin = true))
    assertFalse(shouldShowLibraryAdministration(isAdmin = false))
  }
}
