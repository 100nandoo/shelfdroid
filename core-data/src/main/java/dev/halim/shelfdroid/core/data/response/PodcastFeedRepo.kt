package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PodcastFeedRepo @Inject constructor(private val api: ApiService) {

  val cache: Map<String, PodcastFeed>
    get() = _cache

  private val _cache: MutableMap<String, PodcastFeed> = mutableMapOf()

  suspend fun fetch(url: String): GenericState {
    val result =
      api.podcastFeed(PodcastFeedRequest(url)).getOrNull()
        ?: return GenericState.Failure("Failed to fetch podcast feed.")
    _cache[url] = result
    return GenericState.Success
  }
}
