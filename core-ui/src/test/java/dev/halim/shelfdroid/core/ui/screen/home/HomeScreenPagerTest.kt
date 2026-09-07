package dev.halim.shelfdroid.core.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScreenPagerTest {

  @Test
  fun initialPage_restoresMiscPageAfterHomeRecreation() {
    assertEquals(2, initialHomePage(currentPage = 2, libraryCount = 3))
  }

  @Test
  fun initialPage_keepsDefaultLibraryForNewHomeEntry() {
    assertEquals(1, initialHomePage(currentPage = 0, libraryCount = 3))
  }
}
