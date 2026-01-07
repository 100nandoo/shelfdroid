package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.response.PodcastFeed
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.PodcastFeedRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class AddEpisodeRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val podcastFeedRepo: PodcastFeedRepo,
  private val mapper: AddEpisodeMapper,
  private val json: Json,
) {

  fun item(id: String): AddEpisodeUiState {
    val entity = libraryItemRepo.byId(id) ?: return failureState("Failed to fetch podcast")

    val podcast = decodePodcast(entity.media) ?: return failureState("Invalid podcast data")

    val feedUrl = podcast.metadata.feedUrl
    val podcastFeed: PodcastFeed =
      podcastFeedRepo.cache[feedUrl] ?: return failureState("Failed to fetch podcast feed")

    val episodes: List<AddEpisode> = mapper.mapEpisodes(podcast.episodes, podcastFeed)

    return AddEpisodeUiState(
      state = GenericState.Success,
      author = entity.author,
      title = entity.title,
      cover = entity.cover,
      episodes = episodes,
    )
  }

  private fun decodePodcast(raw: String): Podcast? =
    runCatching { json.decodeFromString<Podcast>(raw) }.getOrNull()

  private fun failureState(message: String) =
    AddEpisodeUiState(state = GenericState.Failure(message))
}
