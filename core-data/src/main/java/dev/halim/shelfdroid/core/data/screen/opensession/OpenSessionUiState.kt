package dev.halim.shelfdroid.core.data.screen.opensession

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session

data class OpenSessionUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: OpenSessionApiState = OpenSessionApiState.Idle,
  val sessions: List<Session> = emptyList(),
)

sealed interface OpenSessionApiState {
  data object Idle : OpenSessionApiState

  data object CloseSuccess : OpenSessionApiState

  data class CloseFailure(val message: String?) : OpenSessionApiState
}
