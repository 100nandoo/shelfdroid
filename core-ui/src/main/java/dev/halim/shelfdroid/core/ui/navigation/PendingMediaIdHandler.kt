package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation.NavHostController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

data class NavRequest(val mediaId: String? = null, val isOpenPlayer: Boolean = true)

fun handlePendingMediaId(
  navRequest: NavRequest,
  isLoggedIn: Boolean,
  navController: NavHostController,
  onNavRequestComplete: () -> Unit,
  viewModel: PlayerViewModel,
) {
  if (navRequest.mediaId == null || !isLoggedIn) return
  val mediaId = navRequest.mediaId

  val request = MediaIdWrapper.fromMediaId(mediaId)
  val secondaryId = request.secondaryId

  if (secondaryId == null || secondaryId.length < 32) {
    navController.navigateOnce(Book(request.itemId), popUpToRoute = Home(false))
  } else {
    navController.navigateOnce(Podcast(request.itemId), Home(false))
    navController.navigateOnce(Episode(request.itemId, secondaryId), Podcast(request.itemId))
  }

  if (navRequest.isOpenPlayer) {
    viewModel.onEvent(PlayerEvent.Big)
  }

  onNavRequestComplete()
}

fun <T : Any> NavHostController.navigateOnce(
  route: T,
  popUpToRoute: T,
  inclusive: Boolean = false,
) {
  val currentRoute = currentBackStackEntry?.destination?.route
  if (currentRoute == route) return

  navigate(route) {
    launchSingleTop = true
    popUpTo(popUpToRoute) { this.inclusive = inclusive }
  }
}
