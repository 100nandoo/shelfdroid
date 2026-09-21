package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.prefs.MediaNotificationTapDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingMediaIdHandlerTest {
  @Test
  fun media_detail_request_for_book_replaces_stack_with_home_then_book() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenMedia(mediaId = "book-id", playerDestination = null),
        isLoggedIn = true,
      )

    assertEquals(
      ResolvedNavRequest(backStack = listOf(Home(false), Book("book-id")), playerDestination = null),
      resolved,
    )
  }

  @Test
  fun media_player_request_for_episode_replaces_stack_with_home_podcast_and_episode() {
    val resolved =
      resolveNavRequest(
        navRequest =
          NavRequest.OpenMedia(
            mediaId = "podcast-id|12345678901234567890123456789012",
            playerDestination = MediaNotificationTapDestination.ExpandedPlayer,
          ),
        isLoggedIn = true,
      )

    assertEquals(
      ResolvedNavRequest(
        backStack =
          listOf(
            Home(false),
            Podcast("podcast-id"),
            Episode("podcast-id", "12345678901234567890123456789012"),
          ),
        playerDestination = MediaNotificationTapDestination.ExpandedPlayer,
      ),
      resolved,
    )
  }

  @Test
  fun player_only_request_opens_home_and_requests_player_open() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenPlayer(MediaNotificationTapDestination.ExpandedPlayer),
        isLoggedIn = true,
      )

    assertEquals(listOf(Home(false)), checkNotNull(resolved).backStack)
    assertEquals(MediaNotificationTapDestination.ExpandedPlayer, resolved.playerDestination)
  }

  @Test
  fun logged_out_requests_are_not_resolved() {
    val resolved =
      resolveNavRequest(
        navRequest =
          NavRequest.OpenMedia(
            mediaId = "book-id",
            playerDestination = MediaNotificationTapDestination.ExpandedPlayer,
          ),
        isLoggedIn = false,
      )

    assertNull(resolved)
  }
}
