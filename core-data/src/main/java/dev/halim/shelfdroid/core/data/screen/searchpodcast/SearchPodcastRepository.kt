package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.ApiService
import javax.inject.Inject

class SearchPodcastRepository
@Inject
constructor(private val api: ApiService, private val mapper: SearchPodcastMapper) {

  suspend fun search(term: String): SearchPodcastUiState {
    val response = api.searchPodcast(term)
    val result = response.getOrNull()

    return if (result != null) {
      val result = mapper.map(result)
      SearchPodcastUiState(state = SearchState.Success, result = result)
    } else {
      SearchPodcastUiState(state = SearchState.Failure(response.exceptionOrNull()?.message))
    }
  }
}
