package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.response.SearchPodcast
import dev.halim.shelfdroid.core.data.response.PodcastInfo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class SearchPodcastMapper @Inject constructor(private val json: Json) {
  fun map(
    response: List<SearchPodcast>,
    podcastInfoList: List<PodcastInfo>,
    libraryId: String,
  ): List<SearchPodcastUi> {
    return response.map { podcast ->
      var id = ""
      val isAdded =
        podcastInfoList.any { podcastInfo ->
          val found =
            podcastInfo.itunesId == podcast.id ||
              podcastInfo.feedUrl == podcast.feedUrl ||
              (podcastInfo.title == podcast.title && podcastInfo.artist == podcast.artistName)
          id = podcastInfo.id
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

      val raw = json.encodeToString(result)
      result.copy(rawJson = raw)
    }
  }
}
