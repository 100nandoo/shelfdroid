package dev.halim.shelfdroid.core.data.screen.episode

import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.util.Locale
import javax.inject.Inject
import kotlinx.serialization.json.Json

class EpisodeRepository
@Inject
constructor(private val libraryItemRepo: LibraryItemRepo, private val progressRepo: ProgressRepo) {
  fun item(itemId: String, episodeId: String): EpisodeUiState {
    val result = libraryItemRepo.byId(itemId)
    val progressEntity = progressRepo.episodeById(episodeId)

    return if (result != null && result.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(result.media)

      val episode =
        media.episodes.find { it.id == episodeId }
          ?: return EpisodeUiState(state = GenericState.Failure("Failed to find episode"))

      val description = episode.description ?: ""

      val progress = progressEntity?.progress?.toFloat() ?: 0f
      val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)

      EpisodeUiState(
        state = GenericState.Success,
        title = episode.title,
        podcast = result.author,
        cover = result.cover,
        description = description,
        progress = formattedProgress,
      )
    } else {
      EpisodeUiState(state = GenericState.Failure("Failed to fetch Podcast"))
    }
  }
}
