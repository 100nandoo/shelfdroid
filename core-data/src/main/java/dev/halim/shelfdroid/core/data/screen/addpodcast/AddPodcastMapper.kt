package dev.halim.shelfdroid.core.data.screen.addpodcast

import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.library.LibraryRepository
import dev.halim.shelfdroid.core.navigation.PodcastSourceFeedNavPayload
import javax.inject.Inject

class AddPodcastMapper @Inject constructor(private val libraryRepository: LibraryRepository) {

  fun map(response: PodcastFeed, payload: PodcastSourceFeedNavPayload): AddPodcastUiState {
    val folders = libraryRepository.listLibraryFolders(payload.libraryId)
    if (folders.isEmpty())
      return AddPodcastUiState(state = AddPodcastState.Failure("No folders found"))

    val metadata = response.podcast.metadata
    val title = metadata.title
    val genres = payload.genre.split(",").map { it.trim() }.distinct()
    return AddPodcastUiState(
      state = AddPodcastState.ApiSourceFeedSuccess,
      title = title,
      author = metadata.author,
      feedUrl = metadata.feedUrl,
      genres = genres,
      type = metadata.type ?: "episodic",
      language = metadata.language,
      explicit = metadata.explicit == "true",
      description = metadata.descriptionPlain,
      folders = folders,
      selectedFolder = folders.first(),
      path = title,
    )
  }
}
