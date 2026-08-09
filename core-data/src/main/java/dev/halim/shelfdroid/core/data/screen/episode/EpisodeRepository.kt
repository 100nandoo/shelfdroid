package dev.halim.shelfdroid.core.data.screen.episode

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.PodcastEpisodeRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class EpisodeRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val podcastEpisodeRepo: PodcastEpisodeRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
  private val prefsRepository: PrefsRepository,
) {
  @OptIn(UnstableApi::class)
  fun item(itemId: String, episodeId: String): Flow<EpisodeUiState> {
    val podcastFlow = libraryItemRepo.flowById(itemId)
    val episodeFlow = podcastEpisodeRepo.flowById(episodeId)
    val progressFlow = progressRepo.flowEpisodeById(episodeId)
    val downloadSignal = combine(downloadRepo.downloads, downloadRepo.durableDownloads) { _, _ -> }
    val userPrefsFlow = prefsRepository.userPrefs
    return combine(
      podcastFlow,
      episodeFlow,
      progressFlow,
      downloadSignal,
      userPrefsFlow,
    ) { podcast, episode, progress, _, userPrefs ->
      podcast
        ?.takeIf { it.isBook.toBoolean().not() }
        ?.let { item ->
          val podcastEpisode =
            episode?.takeIf { it.libraryItemId == item.id }
              ?: return@combine EpisodeUiState(
                state = GenericState.Failure("Failed to find episode")
              )

          val description = podcastEpisode.description ?: ""

          val progress = progress?.progress?.toFloat() ?: 0f
          val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)
          val publishedAt = podcastEpisode.publishedAt?.let { helper.toReadableDate(it) } ?: ""

          val downloadUiState =
            downloadRepo.item(
              itemId = itemId,
              episodeId = episodeId,
              url = podcastEpisode.audioTrack.contentUrl,
              title = podcastEpisode.title,
              secondaryLabel = item.title,
              filename = podcastEpisode.audioTrack.metadata.filename,
            )

          EpisodeUiState(
            state = GenericState.Success,
            title = podcastEpisode.title,
            podcast = item.title,
            publishedAt = publishedAt,
            cover = item.cover,
            description = description,
            progress = formattedProgress,
            canEdit = userPrefs.isAdmin || userPrefs.update,
            download = downloadUiState,
          )
        } ?: EpisodeUiState(state = GenericState.Failure("Failed to fetch Podcast"))
    }
  }
}
