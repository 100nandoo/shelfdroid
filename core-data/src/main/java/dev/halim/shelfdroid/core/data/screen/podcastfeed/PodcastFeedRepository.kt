package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject

class PodcastFeedRepository
@Inject
constructor(private val api: ApiService, private val mapper: PodcastFeedMapper) {

  suspend fun feed(rssFeed: String, libraryId: String): PodcastFeedUiState {
    val response = api.podcastFeed(PodcastFeedRequest(rssFeed))
    val result = response.getOrNull()

    return if (result != null) {
      mapper.map(result, libraryId)
    } else {
      PodcastFeedUiState(state = GenericState.Failure(response.exceptionOrNull()?.message))
    }
  }
}
