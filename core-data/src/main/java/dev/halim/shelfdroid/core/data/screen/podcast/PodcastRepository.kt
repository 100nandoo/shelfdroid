package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject
import kotlinx.serialization.json.Json

class PodcastRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val helper: Helper,
) {

  suspend fun item(id: String): PodcastUiState {
    val entity: LibraryItemEntity? = libraryItemRepo.byId(id)
    val progresses = progressRepo.entities()
    return if (entity != null && entity.isBook == 0L) {
      val media = Json.decodeFromString<Podcast>(entity.media)

      val episodes = mapEpisodes(media.episodes, progresses)
      return PodcastUiState(
        state = GenericState.Success,
        author = entity.author,
        title = entity.title,
        cover = entity.cover,
        description = entity.description,
        episodes = episodes,
      )
    } else {
      PodcastUiState(state = GenericState.Failure("Failed to fetch podcast"))
    }
  }

  private fun mapEpisodes(
    episodes: List<PodcastEpisode>,
    progresses: List<ProgressEntity>,
  ): List<Episode> {
    return episodes
      .sortedByDescending { it.publishedAt }
      .map { podcastEpisode ->
        val progress =
          progresses.find { it.episodeId == podcastEpisode.id }?.progress?.toFloat() ?: 0f

        Episode(
          podcastEpisode.title,
          podcastEpisode.publishedAt?.let { helper.toReadableDate(it) } ?: "",
          progress = progress,
        )
      }
  }
}
