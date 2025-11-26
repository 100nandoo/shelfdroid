package dev.halim.shelfdroid.core.data.screen.podcastfeed

import dev.halim.core.network.response.PodcastFeed
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import javax.inject.Inject

class PodcastFeedMapper @Inject constructor(private val libraryRepo: LibraryRepo) {

  fun map(response: PodcastFeed, libraryId: String): PodcastFeedUiState {
    val folders = libraryRepo.foldersByLibraryId(libraryId)
    if (folders.isEmpty())
      return PodcastFeedUiState(state = GenericState.Failure("No folders found"))

    val metadata = response.podcast.metadata
    val title = metadata.title
    val genres = metadata.categories.flatMap { it.split(":") }.distinct()
    return PodcastFeedUiState(
      state = GenericState.Success,
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
