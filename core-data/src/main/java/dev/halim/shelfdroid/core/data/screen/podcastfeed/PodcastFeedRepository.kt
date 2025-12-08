package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreatePodcastRequest
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.screen.searchpodcast.CreatePodcastResult
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUi
import javax.inject.Inject

class PodcastFeedRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: PodcastFeedMapper,
  private val libraryItemRepo: LibraryItemRepo,
) {

  suspend fun feed(rssFeed: String, searchPodcastUi: SearchPodcastUi): PodcastFeedUiState {
    val response = api.podcastFeed(PodcastFeedRequest(rssFeed))
    val result = response.getOrNull()

    return if (result != null) {
      mapper.map(result, searchPodcastUi)
    } else {
      PodcastFeedUiState(state = PodcastFeedState.Failure(response.exceptionOrNull()?.message))
    }
  }

  suspend fun createPodcast(
    searchPodcastUi: SearchPodcastUi,
    uiState: PodcastFeedUiState,
  ): PodcastFeedUiState {
    val folder = uiState.selectedFolder
    val path = "${folder.path}/${uiState.path}"
    val folderId = folder.id

    val metadata =
      CreatePodcastRequest.Media.Metadata(
        title = uiState.title,
        author = uiState.author,
        description = uiState.description,
        releaseDate = searchPodcastUi.releaseDate,
        genres = uiState.genres,
        feedUrl = uiState.feedUrl,
        imageUrl = searchPodcastUi.cover,
        itunesPageUrl = searchPodcastUi.pageUrl,
        itunesId = searchPodcastUi.itunesId,
        itunesArtistId = searchPodcastUi.itunesArtistId,
        language = uiState.language,
        explicit = uiState.explicit,
        type = uiState.type,
      )
    val media = CreatePodcastRequest.Media(metadata, uiState.autoDownload)
    val request =
      CreatePodcastRequest(
        path = path,
        folderId = folderId,
        libraryId = searchPodcastUi.libraryId,
        media = media,
      )

    val response = api.createPodcast(request)
    val result = response.getOrNull()

    return if (result != null) {
      libraryItemRepo.createPodcast(result, searchPodcastUi.libraryId)
      val result = CreatePodcastResult(result.id, uiState.feedUrl)
      PodcastFeedUiState(state = PodcastFeedState.ApiCreateSuccess(result))
    } else {
      PodcastFeedUiState(state = PodcastFeedState.Failure(response.exceptionOrNull()?.message))
    }
  }
}
