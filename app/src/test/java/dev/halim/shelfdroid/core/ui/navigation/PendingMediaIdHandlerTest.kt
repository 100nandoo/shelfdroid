package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_MINI_PLAYER
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_PLAYER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingMediaIdHandlerTest {
  @Test
  fun stableNotificationActionUsesPresentationExtra() {
    MediaNotificationPlayerPresentation.entries.forEach { presentation ->
      assertEquals(
        NavRequest.OpenMedia(
          mediaId = "book-id",
          playerPresentation = presentation,
          openingScreen = MediaNotificationOpeningScreen.Home,
        ),
        navRequestFromIntent(
          action = ACTION_OPEN_PLAYER,
          mediaId = "book-id",
          openingScreenName = "Home",
          playerPresentationName = presentation.name,
        ),
      )
    }
  }

  @Test
  fun invalidPresentationExtraFallsBackToLegacyAction() {
    assertEquals(
      NavRequest.OpenPlayer(MediaNotificationPlayerPresentation.MiniPlayer),
      navRequestFromIntent(
        action = ACTION_OPEN_MINI_PLAYER,
        mediaId = null,
        openingScreenName = null,
        playerPresentationName = "invalid",
      ),
    )
  }

  @Test
  fun restoredActivityDoesNotReplayACompletedNotificationRequest() {
    assertFalse(shouldHandleLaunchIntent(isFirstCreation = false, hadPendingNavRequest = false))
  }

  @Test
  fun restoredActivityReplaysARequestThatWasStillPending() {
    assertTrue(shouldHandleLaunchIntent(isFirstCreation = false, hadPendingNavRequest = true))
  }

  @Test
  fun firstActivityCreationHandlesItsLaunchIntent() {
    assertTrue(shouldHandleLaunchIntent(isFirstCreation = true, hadPendingNavRequest = false))
  }

  @Test
  fun notificationIntentUsesSelectedOpeningScreenAndPlayerPresentation() {
    assertEquals(
      NavRequest.OpenMedia(
        mediaId = "book-id",
        playerPresentation = MediaNotificationPlayerPresentation.MiniPlayer,
        openingScreen = MediaNotificationOpeningScreen.MediaDetails,
      ),
      navRequestFromIntent(
        action = ACTION_OPEN_MINI_PLAYER,
        mediaId = "book-id",
        openingScreenName = MediaNotificationOpeningScreen.MediaDetails.name,
      ),
    )
  }

  @Test
  fun notificationIntentDefaultsToHomeWhenOpeningScreenIsMissingOrInvalid() {
    listOf(null, "invalid").forEach { openingScreenName ->
      assertEquals(
        NavRequest.OpenMedia(
          mediaId = "book-id",
          playerPresentation = MediaNotificationPlayerPresentation.ExpandedPlayer,
          openingScreen = MediaNotificationOpeningScreen.Home,
        ),
        navRequestFromIntent(
          action = ACTION_OPEN_PLAYER,
          mediaId = "book-id",
          openingScreenName = openingScreenName,
        ),
      )
    }
  }

  @Test
  fun nonNotificationIntentKeepsMediaDetailsBehavior() {
    assertEquals(
      NavRequest.OpenMedia(
        mediaId = "book-id",
        openingScreen = MediaNotificationOpeningScreen.MediaDetails,
      ),
      navRequestFromIntent(
        action = null,
        mediaId = "book-id",
        openingScreenName = "Home",
        playerPresentationName = MediaNotificationPlayerPresentation.ExpandedPlayer.name,
      ),
    )
  }

  @Test
  fun notificationIntentWithoutMediaIdFallsBackToHomePlayerRequest() {
    assertEquals(
      NavRequest.OpenPlayer(MediaNotificationPlayerPresentation.MiniPlayer),
      navRequestFromIntent(
        action = ACTION_OPEN_MINI_PLAYER,
        mediaId = null,
        openingScreenName = MediaNotificationOpeningScreen.MediaDetails.name,
      ),
    )
  }

  @Test
  fun media_detail_request_for_book_replaces_stack_with_home_then_book() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenMedia(mediaId = "book-id", playerPresentation = null),
        isLoggedIn = true,
      )

    assertEquals(
      ResolvedNavRequest(
        backStack = listOf(Home(false), Book("book-id")),
        playerPresentation = null,
      ),
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
            playerPresentation = MediaNotificationPlayerPresentation.ExpandedPlayer,
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
        playerPresentation = MediaNotificationPlayerPresentation.ExpandedPlayer,
      ),
      resolved,
    )
  }

  @Test
  fun player_only_request_opens_home_and_requests_player_open() {
    val resolved =
      resolveNavRequest(
        navRequest = NavRequest.OpenPlayer(MediaNotificationPlayerPresentation.ExpandedPlayer),
        isLoggedIn = true,
      )

    assertEquals(listOf(Home(false)), checkNotNull(resolved).backStack)
    assertEquals(MediaNotificationPlayerPresentation.ExpandedPlayer, resolved.playerPresentation)
  }

  @Test
  fun logged_out_requests_are_not_resolved() {
    val resolved =
      resolveNavRequest(
        navRequest =
          NavRequest.OpenMedia(
            mediaId = "book-id",
            playerPresentation = MediaNotificationPlayerPresentation.ExpandedPlayer,
          ),
        isLoggedIn = false,
      )

    assertNull(resolved)
  }

  @Test
  fun openingScreenAndPlayerPresentationAreIndependent() {
    data class Case(
      val mediaId: String,
      val openingScreen: MediaNotificationOpeningScreen,
      val playerPresentation: MediaNotificationPlayerPresentation,
      val backStack: List<ShelfNavKey>,
    )

    val episodeMediaId = "podcast-id|12345678901234567890123456789012"
    val episodeDetailsBackStack =
      listOf(
        Home(false),
        Podcast("podcast-id"),
        Episode("podcast-id", "12345678901234567890123456789012"),
      )
    val cases =
      listOf(
        Case(
          "book-id",
          MediaNotificationOpeningScreen.Home,
          MediaNotificationPlayerPresentation.ExpandedPlayer,
          listOf(Home(false)),
        ),
        Case(
          "book-id",
          MediaNotificationOpeningScreen.Home,
          MediaNotificationPlayerPresentation.MiniPlayer,
          listOf(Home(false)),
        ),
        Case(
          "book-id",
          MediaNotificationOpeningScreen.MediaDetails,
          MediaNotificationPlayerPresentation.ExpandedPlayer,
          listOf(Home(false), Book("book-id")),
        ),
        Case(
          "book-id",
          MediaNotificationOpeningScreen.MediaDetails,
          MediaNotificationPlayerPresentation.MiniPlayer,
          listOf(Home(false), Book("book-id")),
        ),
        Case(
          episodeMediaId,
          MediaNotificationOpeningScreen.Home,
          MediaNotificationPlayerPresentation.ExpandedPlayer,
          listOf(Home(false)),
        ),
        Case(
          episodeMediaId,
          MediaNotificationOpeningScreen.Home,
          MediaNotificationPlayerPresentation.MiniPlayer,
          listOf(Home(false)),
        ),
        Case(
          episodeMediaId,
          MediaNotificationOpeningScreen.MediaDetails,
          MediaNotificationPlayerPresentation.ExpandedPlayer,
          episodeDetailsBackStack,
        ),
        Case(
          episodeMediaId,
          MediaNotificationOpeningScreen.MediaDetails,
          MediaNotificationPlayerPresentation.MiniPlayer,
          episodeDetailsBackStack,
        ),
      )

    cases.forEach { case ->
      val resolved =
        resolveNavRequest(
          navRequest =
            NavRequest.OpenMedia(
              mediaId = case.mediaId,
              playerPresentation = case.playerPresentation,
              openingScreen = case.openingScreen,
            ),
          isLoggedIn = true,
        )

      assertEquals(ResolvedNavRequest(case.backStack, case.playerPresentation), resolved)
    }
  }
}
