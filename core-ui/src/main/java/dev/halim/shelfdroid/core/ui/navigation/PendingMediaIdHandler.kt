package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation.NavHostController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.media.mediaitem.MediaIdWrapper

fun handlePendingMediaId(
  pendingMediaId: String?,
  isLoggedIn: Boolean,
  navController: NavHostController,
  onMediaIdHandled: () -> Unit,
  viewModel: PlayerViewModel,
) {
  if (pendingMediaId == null || !isLoggedIn) return

  val request = MediaIdWrapper.fromMediaId(pendingMediaId)
  val episodeId = request.episodeId

  if (episodeId == null) {
    navController.navigateOnce(Book(pendingMediaId), popUpToRoute = Home)
  } else {
    navController.navigateOnce(Podcast(request.itemId), Home)
    navController.navigateOnce(Episode(request.itemId, episodeId), Podcast(request.itemId))
  }

  onMediaIdHandled()
  viewModel.onEvent(PlayerEvent.Big)
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
