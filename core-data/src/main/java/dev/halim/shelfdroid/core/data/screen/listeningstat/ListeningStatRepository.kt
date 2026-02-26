package dev.halim.shelfdroid.core.data.screen.listeningstat

import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.ListeningStatRepo
import javax.inject.Inject

class ListeningStatRepository
@Inject
constructor(private val repo: ListeningStatRepo, private val mapper: ListeningStatMapper) {

  fun item(userId: String): ListeningStatUiState {
    val entity =
      repo.byUserId(userId) ?: return ListeningStatUiState(state = GenericState.Failure())

    val uiState = mapper.toUiState(entity)
    return uiState
  }
}
