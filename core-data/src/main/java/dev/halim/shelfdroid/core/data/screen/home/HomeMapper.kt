package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.database.LibraryItemCatalog
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject

class HomeMapper @Inject constructor(private val progressRepo: ProgressRepository) {

  suspend fun toBookUiState(item: LibraryItemCatalog, isDownloaded: Boolean): BookUiState {
    val progress = progressRepo.bookById(item.id)
    return BookUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      addedAt = item.addedAt,
      isDownloaded = isDownloaded,
      progressLastUpdate = progress?.lastUpdate ?: 0,
    )
  }

  suspend fun toPodcastUiState(
    item: LibraryItemCatalog,
    downloadedEpisodeIds: Set<String>,
  ): PodcastUiState {
    val progresses = progressRepo.byLibraryItemId(item.id)

    val finished = progressRepo.byLibraryItemIdAndFinished(item.id)
    val finishedCount = finished.count()
    val downloadCounts =
      podcastDownloadCounts(
        downloadedEpisodeIds = downloadedEpisodeIds,
        finishedEpisodeIds = finished.mapNotNull { it.episodeId }.toSet(),
      )

    val episodeCount = item.episodeCount.toInt()
    val unfinishedCount = (episodeCount - finishedCount).coerceAtLeast(0)

    return PodcastUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      addedAt = item.addedAt,
      progressLastUpdate = podcastProgressLastUpdate(progresses),
      episodeCount = episodeCount,
      unfinishedCount = unfinishedCount,
      downloadedCount = downloadCounts.downloadedCount,
      unfinishedAndDownloadCount = downloadCounts.unfinishedAndDownloadCount,
    )
  }
}

internal data class PodcastDownloadCounts(
  val downloadedCount: Int,
  val unfinishedAndDownloadCount: Int,
)

internal fun podcastDownloadCounts(
  downloadedEpisodeIds: Set<String>,
  finishedEpisodeIds: Set<String>,
): PodcastDownloadCounts {
  return PodcastDownloadCounts(
    downloadedCount = downloadedEpisodeIds.size,
    unfinishedAndDownloadCount = downloadedEpisodeIds.count { it !in finishedEpisodeIds },
  )
}

internal fun isBookFullyDownloaded(
  trackFilenames: List<String>,
  downloadedTrackFilenames: Set<String>,
): Boolean {
  return trackFilenames.isNotEmpty() && trackFilenames.all { it in downloadedTrackFilenames }
}

internal fun podcastProgressLastUpdate(progresses: List<ProgressEntity>): Long {
  return progresses.filter { !it.episodeId.isNullOrBlank() }.maxOfOrNull(ProgressEntity::lastUpdate)
    ?: 0L
}
