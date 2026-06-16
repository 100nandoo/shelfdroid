package dev.halim.shelfdroid.core.ui.player

import dev.halim.shelfdroid.core.ui.navigation.Book
import dev.halim.shelfdroid.core.ui.navigation.Home
import dev.halim.shelfdroid.core.ui.navigation.Login
import dev.halim.shelfdroid.core.ui.navigation.Settings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerVisibilityRouteTest {
  @Test
  fun player_is_visible_on_home_and_book_keys() {
    assertTrue(isPlayerVisibleDestination(Home(fromLogin = false)))
    assertTrue(isPlayerVisibleDestination(Book(id = "book-id")))
  }

  @Test
  fun player_is_hidden_on_login_and_settings_keys() {
    assertFalse(isPlayerVisibleDestination(Login()))
    assertFalse(isPlayerVisibleDestination(Settings))
  }
}
