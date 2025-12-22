package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import javax.inject.Inject

class PodcastFeedMapper @Inject constructor(private val libraryRepo: LibraryRepo) {

  fun map(response: PodcastFeed, payload: PodcastFeedNavPayload): PodcastFeedUiState {
    val folders = libraryRepo.foldersByLibraryId(payload.libraryId)
    if (folders.isEmpty())
      return PodcastFeedUiState(state = PodcastFeedState.Failure("No folders found"))

    val metadata = response.podcast.metadata
    val title = metadata.title
    val genres = payload.genre.split(",").map { it.trim() }.distinct()
    return PodcastFeedUiState(
      state = PodcastFeedState.ApiFeedSuccess,
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
