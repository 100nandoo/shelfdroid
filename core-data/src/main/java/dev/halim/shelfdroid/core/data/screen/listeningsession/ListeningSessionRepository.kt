package dev.halim.shelfdroid.core.data.screen.listeningsession

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class ListeningSessionRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: ListeningSessionMapper,
  private val prefsRepository: PrefsRepository,
) {
  val listeningSessionPrefs = prefsRepository.listeningSessionPrefs

  suspend fun item(page: Int = 0, itemsPerPage: Int = 10): ListeningSessionUiState {
    val response =
      api.sessions(page = page, itemsPerPage = itemsPerPage).getOrElse {
        return ListeningSessionUiState(state = GenericState.Failure(it.message))
      }
    val result = mapper.map(response)
    return result
  }

  suspend fun updateItemsPerPage(itemsPerPage: Int) {
    prefsRepository.updateListeningSessionPrefs(
      listeningSessionPrefs.first().copy(itemsPerPage = itemsPerPage)
    )
  }
}
