package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject

class ListeningSessionRepository
@Inject
constructor(private val api: ApiService, private val mapper: ListeningSessionMapper) {

  suspend fun item(page: Int = 0): ListeningSessionUiState {
    val response = api.sessions(page = page).getOrNull()
    if (response == null)
      return ListeningSessionUiState(state = GenericState.Failure("Get all sessions failed."))
    val result = mapper.map(response)
    return result
  }
}
