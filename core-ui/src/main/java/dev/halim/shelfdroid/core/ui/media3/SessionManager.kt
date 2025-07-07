package dev.halim.shelfdroid.core.ui.media3

import dev.halim.shelfdroid.core.data.screen.player.PlayerRepository
import dev.halim.shelfdroid.core.data.screen.player.PlayerUiState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

sealed class SessionEvent {
  data class Play(val uiState: PlayerUiState) : SessionEvent()

  data class Pause(val uiState: PlayerUiState) : SessionEvent()
}

@Singleton
class SessionManager @Inject constructor(private val repository: PlayerRepository) {

  private var sessionId: String = ""

  fun onEvent(event: SessionEvent, io: CoroutineScope) {
    when (event) {
      is SessionEvent.Pause -> {
        syncSession(io, event.uiState)
      }
      is SessionEvent.Play -> {
        if (sessionId != event.uiState.sessionId) {
          syncSession(io, event.uiState)
        }
        sessionId = event.uiState.sessionId
      }
    }
  }

  private fun syncSession(io: CoroutineScope, uiState: PlayerUiState) {
    io.launch {
      val result = repository.syncSession(sessionId, uiState)
    }
  }
}
