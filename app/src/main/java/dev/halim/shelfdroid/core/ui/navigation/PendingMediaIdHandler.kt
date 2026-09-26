package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.prefs.MediaNotificationOpeningScreen
import dev.halim.shelfdroid.core.prefs.MediaNotificationPlayerPresentation
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_MINI_PLAYER
import dev.halim.shelfdroid.helper.Helper.Companion.ACTION_OPEN_PLAYER
import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

sealed interface NavRequest {
  data object None : NavRequest

  data class OpenPlayer(val presentation: MediaNotificationPlayerPresentation) : NavRequest

  data class OpenMedia(
    val mediaId: String,
    val playerPresentation: MediaNotificationPlayerPresentation? = null,
    val openingScreen: MediaNotificationOpeningScreen = MediaNotificationOpeningScreen.MediaDetails,
  ) : NavRequest
}

data class ResolvedNavRequest(
  val backStack: List<ShelfNavKey>,
  val playerPresentation: MediaNotificationPlayerPresentation?,
)

internal fun shouldHandleLaunchIntent(
  isFirstCreation: Boolean,
  hadPendingNavRequest: Boolean,
): Boolean = isFirstCreation || hadPendingNavRequest

fun navRequestFromIntent(
  action: String?,
  mediaId: String?,
  openingScreenName: String?,
): NavRequest {
  val playerPresentation =
    when (action) {
      ACTION_OPEN_PLAYER -> MediaNotificationPlayerPresentation.ExpandedPlayer
      ACTION_OPEN_MINI_PLAYER -> MediaNotificationPlayerPresentation.MiniPlayer
      else -> null
    }
  val openingScreen =
    if (playerPresentation != null) {
      openingScreenName?.let { value ->
        MediaNotificationOpeningScreen.entries.firstOrNull { it.name == value }
      } ?: MediaNotificationOpeningScreen.Home
    } else {
      MediaNotificationOpeningScreen.MediaDetails
    }

  return when {
    !mediaId.isNullOrBlank() ->
      NavRequest.OpenMedia(
        mediaId = mediaId,
        playerPresentation = playerPresentation,
        openingScreen = openingScreen,
      )
    playerPresentation != null -> NavRequest.OpenPlayer(playerPresentation)
    else -> NavRequest.None
  }
}

fun handleNavRequest(
  navRequest: NavRequest,
  isLoggedIn: Boolean,
  navigator: ShelfNavigator,
  onNavRequestComplete: () -> Unit,
  playerController: PlayerController,
) {
  val resolved = resolveNavRequest(navRequest, isLoggedIn) ?: return

  if (resolved.backStack.isNotEmpty()) {
    navigator.replaceStackPreservingHome(resolved.backStack)
  }
  if (playerController.hasCurrentPlayback()) {
    when (resolved.playerPresentation) {
      MediaNotificationPlayerPresentation.ExpandedPlayer ->
        playerController.onEvent(PlayerEvent.Big)
      MediaNotificationPlayerPresentation.MiniPlayer -> playerController.onEvent(PlayerEvent.Small)
      null -> Unit
    }
  }

  onNavRequestComplete()
}

fun resolveNavRequest(navRequest: NavRequest, isLoggedIn: Boolean): ResolvedNavRequest? {
  if (!isLoggedIn) return null

  return when (navRequest) {
    NavRequest.None -> null
    is NavRequest.OpenPlayer ->
      ResolvedNavRequest(
        backStack = listOf(Home(false)),
        playerPresentation = navRequest.presentation,
      )
    is NavRequest.OpenMedia -> {
      val request = MediaIdWrapper.fromMediaId(navRequest.mediaId)
      val secondaryId = request.secondaryId
      val backStack =
        when (navRequest.openingScreen) {
          MediaNotificationOpeningScreen.Home -> listOf(Home(false))
          MediaNotificationOpeningScreen.MediaDetails ->
            if (secondaryId == null || secondaryId.length < 32) {
              listOf(Home(false), Book(request.itemId))
            } else {
              listOf(Home(false), Podcast(request.itemId), Episode(request.itemId, secondaryId))
            }
        }

      ResolvedNavRequest(backStack = backStack, playerPresentation = navRequest.playerPresentation)
    }
  }
}
