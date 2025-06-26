package dev.halim.shelfdroid.core.ui.navigation

import androidx.navigation.NavHostController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel

data class MediaIdWrapper(val itemId: String, val episodeId: String? = null) {
  fun toMediaId(): String = episodeId?.let { "$itemId|$it" } ?: itemId

  companion object {
    fun fromMediaId(mediaId: String): MediaIdWrapper {
      val parts = mediaId.split("|")
      return if (parts.size == 2) MediaIdWrapper(parts[0], parts[1])
      else MediaIdWrapper(parts[0], null)
    }
  }
}

fun handlePendingMediaId(
  pendingMediaId: String?,
  isLoggedIn: Boolean,
  navController: NavHostController,
  onMediaIdHandled: () -> Unit,
  viewModel: PlayerViewModel,
) {
  if (pendingMediaId == null || !isLoggedIn) return
  val request = MediaIdWrapper.fromMediaId(pendingMediaId)

  if (request.episodeId == null) {
    navController.navigate(Book(pendingMediaId)) { popUpTo(Home) { inclusive = false } }
  } else {
    navController.navigate(Podcast(request.itemId)) { launchSingleTop = true }
    navController.navigate(Episode(request.itemId, request.episodeId)) { launchSingleTop = true }
  }
  onMediaIdHandled()
  viewModel.onEvent(PlayerEvent.Small)
}
