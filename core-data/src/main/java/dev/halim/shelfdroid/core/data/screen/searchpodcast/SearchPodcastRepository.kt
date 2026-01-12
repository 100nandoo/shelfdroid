package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject

class SearchPodcastRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: SearchPodcastMapper,
  private val libraryItemRepo: LibraryItemRepo,
) {

  suspend fun search(term: String, libraryId: String): SearchPodcastUiState {
    val response = api.searchPodcast(term)
    val result = response.getOrNull()
    val podcastInfoList = libraryItemRepo.podcastInfoList(libraryId)
    return if (result != null) {
      val result = mapper.map(result, podcastInfoList, libraryId)
      SearchPodcastUiState(state = GenericState.Success, result = result)
    } else {
      SearchPodcastUiState(state = GenericState.Failure(response.exceptionOrNull()?.message))
    }
  }
}
