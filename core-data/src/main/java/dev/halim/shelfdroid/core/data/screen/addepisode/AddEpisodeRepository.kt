package dev.halim.shelfdroid.core.data.screen.addepisode

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class AddEpisodeRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val api: ApiService,
  private val mapper: AddEpisodeMapper,
  private val json: Json,
) {

  suspend fun item(id: String): AddEpisodeUiState {
    val entity = libraryItemRepo.byId(id) ?: return failureState("Failed to fetch podcast")

    val podcast = decodePodcast(entity.media) ?: return failureState("Invalid podcast data")

    val feedUrl = podcast.metadata.feedUrl ?: return failureState("Podcast feed URL not found")

    val episodes =
      fetchEpisodes(podcast.episodes, feedUrl) ?: return failureState("No episodes found")

    return AddEpisodeUiState(
      state = GenericState.Success,
      author = entity.author,
      title = entity.title,
      cover = entity.cover,
      episodes = episodes,
    )
  }

  private suspend fun fetchEpisodes(
    episodesFromDb: List<PodcastEpisode>,
    feedUrl: String,
  ): List<Episode>? = episodes(episodesFromDb, feedUrl).getOrNull()

  private suspend fun episodes(
    episodesFromDb: List<PodcastEpisode>,
    rssFeed: String,
  ): Result<List<Episode>> {
    return api.podcastFeed(PodcastFeedRequest(rssFeed)).mapCatching { response ->
      val episodes = response.podcast.episodes
      require(episodes.isNotEmpty()) { "No episodes found" }
      mapper.mapEpisodes(episodesFromDb, response)
    }
  }

  private fun decodePodcast(raw: String): Podcast? =
    runCatching { json.decodeFromString<Podcast>(raw) }.getOrNull()

  private fun failureState(message: String) =
    AddEpisodeUiState(state = GenericState.Failure(message))
}
