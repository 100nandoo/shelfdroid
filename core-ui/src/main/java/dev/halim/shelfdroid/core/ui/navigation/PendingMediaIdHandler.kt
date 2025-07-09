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
    navController.navigate(Book(pendingMediaId)) { popUpTo(Home) { inclusive = false } }
  } else {
    navController.navigate(Podcast(request.itemId)) { launchSingleTop = true }
    navController.navigate(Episode(request.itemId, episodeId)) { launchSingleTop = true }
  }
  onMediaIdHandled()
  viewModel.onEvent(PlayerEvent.Small)
}
