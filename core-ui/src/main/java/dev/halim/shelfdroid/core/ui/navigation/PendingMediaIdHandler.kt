package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.prefs.MediaNotificationTapDestination
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

sealed interface NavRequest {
  data object None : NavRequest

  data class OpenPlayer(val destination: MediaNotificationTapDestination) : NavRequest

  data class OpenMedia(
    val mediaId: String,
    val playerDestination: MediaNotificationTapDestination?,
  ) : NavRequest
}

data class ResolvedNavRequest(
  val backStack: List<ShelfNavKey>,
  val playerDestination: MediaNotificationTapDestination?,
)

fun handleNavRequest(
  navRequest: NavRequest,
  isLoggedIn: Boolean,
  navigator: ShelfNavigator,
  onNavRequestComplete: () -> Unit,
  playerController: PlayerController,
) {
  val resolved = resolveNavRequest(navRequest, isLoggedIn) ?: return

  if (resolved.backStack.isNotEmpty()) {
    navigator.replaceStack(resolved.backStack)
  }
  if (playerController.hasCurrentPlayback()) {
    when (resolved.playerDestination) {
      MediaNotificationTapDestination.ExpandedPlayer -> playerController.onEvent(PlayerEvent.Big)
      MediaNotificationTapDestination.MiniPlayer -> playerController.onEvent(PlayerEvent.Small)
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
        playerDestination = navRequest.destination,
      )
    is NavRequest.OpenMedia -> {
      val request = MediaIdWrapper.fromMediaId(navRequest.mediaId)
      val secondaryId = request.secondaryId
      val backStack =
        if (secondaryId == null || secondaryId.length < 32) {
          listOf(Home(false), Book(request.itemId))
        } else {
          listOf(Home(false), Podcast(request.itemId), Episode(request.itemId, secondaryId))
        }

      ResolvedNavRequest(backStack = backStack, playerDestination = navRequest.playerDestination)
    }
  }
}
