package dev.halim.shelfdroid.core.data.session

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.SyncSessionRequest
import dev.halim.shelfdroid.core.PlayerInternalStateHolder
import dev.halim.shelfdroid.core.PlayerUiState
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.screen.player.PlayerFinder
import javax.inject.Inject
import kotlin.time.Duration

class RemoteSessionRepo
@Inject
constructor(
  private val apiService: ApiService,
  private val finder: PlayerFinder,
  private val progressRepo: ProgressRepo,
  private val state: PlayerInternalStateHolder,
) {

  suspend fun syncSession(uiState: PlayerUiState, rawPositionMs: Long, duration: Duration) {
    val currentTime = finder.bookPosition(state.startOffset(), rawPositionMs)
    val request = SyncSessionRequest(currentTime.toLong(), duration.inWholeSeconds)
    val result = apiService.syncSession(state.sessionId(), request).getOrNull()
    if (result == null) return
    val entity =
      if (state.isBook()) progressRepo.bookById(uiState.id)
      else progressRepo.episodeById(uiState.episodeId)
    entity?.let {
      val progress = currentTime / it.duration
      val updated = it.copy(currentTime = currentTime, progress = progress)
      progressRepo.updateProgress(updated)
    }
  }
}
