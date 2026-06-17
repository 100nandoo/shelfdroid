package dev.halim.shelfdroid.core.ui.navigation

import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

sealed interface NavRequest {
  data object None : NavRequest

  data object OpenPlayer : NavRequest

  data class OpenMedia(val mediaId: String, val openPlayer: Boolean) : NavRequest
}

data class ResolvedNavRequest(val backStack: List<ShelfNavKey>, val openPlayer: Boolean)

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
  if (resolved.openPlayer) {
    playerController.onEvent(PlayerEvent.Big)
  }

  onNavRequestComplete()
}

fun resolveNavRequest(navRequest: NavRequest, isLoggedIn: Boolean): ResolvedNavRequest? {
  if (!isLoggedIn) return null

  return when (navRequest) {
    NavRequest.None -> null
    NavRequest.OpenPlayer -> ResolvedNavRequest(backStack = emptyList(), openPlayer = true)
    is NavRequest.OpenMedia -> {
      val request = MediaIdWrapper.fromMediaId(navRequest.mediaId)
      val secondaryId = request.secondaryId
      val backStack =
        if (secondaryId == null || secondaryId.length < 32) {
          listOf(Home(false), Book(request.itemId))
        } else {
          listOf(Home(false), Podcast(request.itemId), Episode(request.itemId, secondaryId))
        }

      ResolvedNavRequest(backStack = backStack, openPlayer = navRequest.openPlayer)
    }
  }
}
