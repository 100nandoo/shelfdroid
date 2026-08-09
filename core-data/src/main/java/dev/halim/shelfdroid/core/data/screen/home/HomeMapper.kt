package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemCatalog
import dev.halim.shelfdroid.core.database.ProgressEntity
import javax.inject.Inject

class HomeMapper @Inject constructor(private val progressRepo: ProgressRepo) {

  suspend fun toBookUiState(item: LibraryItemCatalog): BookUiState {
    val progress = progressRepo.bookById(item.id)
    return BookUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      addedAt = item.addedAt,
      progressLastUpdate = progress?.lastUpdate ?: 0,
    )
  }

  suspend fun toPodcastUiState(item: LibraryItemCatalog): PodcastUiState {
    val progresses = progressRepo.byLibraryItemId(item.id)

    val finished = progressRepo.byLibraryItemIdAndFinished(item.id)
    val finishedCount = finished.count()

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
    )
  }
}

internal fun podcastProgressLastUpdate(progresses: List<ProgressEntity>): Long {
  return progresses.filter { !it.episodeId.isNullOrBlank() }.maxOfOrNull(ProgressEntity::lastUpdate)
    ?: 0L
}
