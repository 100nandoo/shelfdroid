package dev.halim.shelfdroid.core.data.screen.addpodcast

import dev.halim.core.network.ApiService
import dev.halim.core.network.request.CreatePodcastRequest
import dev.halim.core.network.request.PodcastFeedRequest
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.navigation.CreatePodcastNavResult
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import javax.inject.Inject

class AddPodcastRepository
@Inject
constructor(
  private val api: ApiService,
  private val mapper: AddPodcastMapper,
  private val libraryItemRepo: LibraryItemRepo,
) {

  suspend fun feed(rssFeed: String, payload: PodcastFeedNavPayload): AddPodcastUiState {
    val response = api.podcastFeed(PodcastFeedRequest(rssFeed))
    val result = response.getOrNull()

    return if (result != null) {
      mapper.map(result, payload)
    } else {
      AddPodcastUiState(state = AddPodcastState.Failure(response.exceptionOrNull()?.message))
    }
  }

  suspend fun createPodcast(
    payload: PodcastFeedNavPayload,
    uiState: AddPodcastUiState,
  ): AddPodcastUiState {
    val folder = uiState.selectedFolder
    val path = "${folder.path}/${uiState.path}"
    val folderId = folder.id

    val metadata =
      CreatePodcastRequest.Media.Metadata(
        title = uiState.title,
        author = uiState.author,
        description = uiState.description,
        releaseDate = payload.releaseDate,
        genres = uiState.genres,
        feedUrl = uiState.feedUrl,
        imageUrl = payload.cover,
        itunesPageUrl = payload.pageUrl,
        itunesId = payload.itunesId,
        itunesArtistId = payload.itunesArtistId,
        language = uiState.language,
        explicit = uiState.explicit,
        type = uiState.type,
      )
    val media = CreatePodcastRequest.Media(metadata, uiState.autoDownload)
    val request =
      CreatePodcastRequest(
        path = path,
        folderId = folderId,
        libraryId = payload.libraryId,
        media = media,
      )

    val response = api.createPodcast(request)
    val result = response.getOrNull()

    return if (result != null) {
      libraryItemRepo.createPodcast(result, payload.libraryId)
      val result = CreatePodcastNavResult(result.id, uiState.feedUrl)
      AddPodcastUiState(state = AddPodcastState.ApiCreateSuccess(result))
    } else {
      AddPodcastUiState(state = AddPodcastState.Failure(response.exceptionOrNull()?.message))
    }
  }
}
