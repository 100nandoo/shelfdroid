@file:OptIn(kotlin.time.ExperimentalTime::class)

package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.GenericState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

@Singleton
class PodcastFeedRepo @Inject constructor(private val api: ApiService) {

  val cache: Map<String, PodcastFeed>
    get() = _cache.mapValues { it.value.feed }

  private val _cache: MutableMap<String, CacheEntry> = mutableMapOf()

  suspend fun fetch(url: String): GenericState {
    val cachedEntry = _cache[url]
    val now = Clock.System.now()
    if (cachedEntry != null && (now - cachedEntry.timestamp) < 1.hours) {
      return GenericState.Success
    }

    val result =
      api.podcastFeed(PodcastFeedRequest(url)).getOrNull()
        ?: return GenericState.Failure("Failed to fetch podcast feed.")

    _cache[url] = CacheEntry(result, now)
    return GenericState.Success
  }

  private data class CacheEntry(val feed: PodcastFeed, val timestamp: Instant)
}
