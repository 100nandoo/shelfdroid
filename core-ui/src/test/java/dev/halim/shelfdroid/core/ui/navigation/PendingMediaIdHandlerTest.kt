package dev.halim.shelfdroid.core.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingMediaIdHandlerTest {
  @Test
  fun media_detail_request_for_book_replaces_stack_with_home_then_book() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenMedia(mediaId = "book-id", openPlayer = false),
        isLoggedIn = true,
      )

    assertEquals(
      ResolvedNavRequest(backStack = listOf(Home(false), Book("book-id")), openPlayer = false),
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
            openPlayer = true,
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
        openPlayer = true,
      ),
      resolved,
    )
  }

  @Test
  fun player_only_request_keeps_current_stack_and_requests_player_open() {
    val resolved = resolveNavRequest(navRequest = NavRequest.OpenPlayer, isLoggedIn = true)

    assertTrue(checkNotNull(resolved).backStack.isEmpty())
    assertTrue(resolved.openPlayer)
  }

  @Test
  fun logged_out_requests_are_not_resolved() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenMedia(mediaId = "book-id", openPlayer = true),
        isLoggedIn = false,
      )

    assertNull(resolved)
  }
}
