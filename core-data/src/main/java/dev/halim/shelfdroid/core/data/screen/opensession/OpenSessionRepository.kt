package dev.halim.shelfdroid.core.data.screen.opensession

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.mapper.SessionMapper
import javax.inject.Inject

class OpenSessionRepository
@Inject
constructor(private val api: ApiService, private val sessionMapper: SessionMapper) {

  suspend fun openSessions(): OpenSessionUiState {
    val response =
      api.openSessions().getOrElse {
        return OpenSessionUiState(state = GenericState.Failure())
      }

    val result = sessionMapper.sessions(response.sessions)
    return OpenSessionUiState(state = GenericState.Success, sessions = result)
  }

  suspend fun closeSession(sessionId: String, uiState: OpenSessionUiState): OpenSessionUiState {
    api.closeSession(sessionId).getOrElse {
      return uiState.copy(apiState = OpenSessionApiState.CloseFailure(it.message))
    }

    val updatedSessions = uiState.sessions.filter { it.id != sessionId }
    return uiState.copy(apiState = OpenSessionApiState.CloseSuccess, sessions = updatedSessions)
  }
}
