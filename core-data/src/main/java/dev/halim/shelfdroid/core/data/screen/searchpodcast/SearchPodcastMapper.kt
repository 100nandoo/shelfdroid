package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.response.SearchPodcast
import dev.halim.shelfdroid.core.data.library.ExistingPodcastSummary
import dev.halim.shelfdroid.core.navigation.PodcastSourceFeedNavPayload
import javax.inject.Inject

class SearchPodcastMapper @Inject constructor() {
  fun map(
    response: List<SearchPodcast>,
    existingPodcastSummaries: List<ExistingPodcastSummary>,
    libraryId: String,
  ): List<SearchPodcastUi> {
    return response.map { podcast ->
      var id = ""
      val isAdded = existingPodcastSummaries.any { existingPodcastSummary ->
        val found =
          existingPodcastSummary.itunesId == podcast.id.toString() ||
            existingPodcastSummary.feedUrl == podcast.feedUrl ||
            (existingPodcastSummary.title == podcast.title &&
              existingPodcastSummary.artist == podcast.artistName)
        id = existingPodcastSummary.id
        found
      }

      val result =
        SearchPodcastUi(
          id = id,
          itunesId = podcast.id,
          itunesArtistId = podcast.artistId,
          libraryId = libraryId,
          author = podcast.artistName,
          title = podcast.title,
          cover = podcast.cover,
          genre = podcast.genres.joinToString(),
          episodeCount = podcast.trackCount,
          feedUrl = podcast.feedUrl,
          pageUrl = podcast.pageUrl,
          releaseDate = podcast.releaseDate,
          explicit = podcast.explicit,
          isAdded = isAdded,
        )

      val payload = toPayload(result)
      result.copy(payload = payload)
    }
  }

  fun toPayload(model: SearchPodcastUi): PodcastSourceFeedNavPayload =
    PodcastSourceFeedNavPayload(
      id = model.id,
      itunesId = model.itunesId,
      itunesArtistId = model.itunesArtistId,
      libraryId = model.libraryId,
      author = model.author,
      title = model.title,
      cover = model.cover,
      genre = model.genre,
      episodeCount = model.episodeCount,
      feedUrl = model.feedUrl,
      pageUrl = model.pageUrl,
      releaseDate = model.releaseDate,
      explicit = model.explicit,
      isAdded = model.isAdded,
    )
}
