package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUi
import javax.inject.Inject

class PodcastFeedMapper @Inject constructor(private val libraryRepo: LibraryRepo) {

  fun map(response: PodcastFeed, searchPodcastUi: SearchPodcastUi): PodcastFeedUiState {
    val folders = libraryRepo.foldersByLibraryId(searchPodcastUi.libraryId)
    if (folders.isEmpty())
      return PodcastFeedUiState(state = PodcastFeedState.Failure("No folders found"))

    val metadata = response.podcast.metadata
    val title = metadata.title
    val genres = searchPodcastUi.genre.split(",").map { it.trim() }.distinct()
    return PodcastFeedUiState(
      state = PodcastFeedState.ApiFeedSuccess,
      title = title,
      author = metadata.author,
      feedUrl = metadata.feedUrl,
      genres = genres,
      type = metadata.type,
      language = metadata.language,
      explicit = metadata.explicit == "true",
      description = metadata.descriptionPlain,
      folders = folders,
      selectedFolder = folders.first(),
      path = title,
    )
  }
}
