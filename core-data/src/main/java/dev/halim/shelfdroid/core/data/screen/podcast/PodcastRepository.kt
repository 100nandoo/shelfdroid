package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

class PodcastRepository
@Inject
constructor(private val libraryItemRepo: LibraryItemRepo, private val helper: Helper) {

  fun item(id: String): PodcastUiState {
    val result = libraryItemRepo.byId(id)
    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)
      return PodcastUiState(
        state = GenericState.Success,
        author = result.author,
        title = result.title,
        cover = result.cover,
        description = result.description,
        episodes =
          media.episodes.map {
            Episode(
              it.title,
              it.publishedAt?.let { helper.toReadableDate(it) } ?: "",
              progress = 0.15f,
            )
          },
      )
    } else {
      PodcastUiState(state = GenericState.Failure("Failed to fetch podcast"))
    }
  }
}
