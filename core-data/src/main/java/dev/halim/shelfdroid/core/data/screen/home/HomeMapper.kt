package dev.halim.shelfdroid.core.data.screen.home

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

@SuppressLint("UnsafeOptInUsageError")
class HomeMapper
@Inject
constructor(private val progressRepo: ProgressRepo, private val downloadRepo: DownloadRepo) {

  suspend fun toBookUiState(item: LibraryItemEntity): BookUiState {
    val progress = progressRepo.bookById(item.id)
    val book = Json.decodeFromString<Book>(item.media)
    val trackIndexes = book.audioTracks.map { it.index }
    val isDownloaded = downloadRepo.isBookDownloaded(item.title, item.author, book.audioTracks)
    return BookUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      duration = book.duration ?: 0.0,
      addedAt = item.addedAt,
      isDownloaded = isDownloaded,
      trackIndexes = trackIndexes,
      progressLastUpdate = progress?.lastUpdate ?: 0,
    )
  }

  suspend fun toPodcastUiState(item: LibraryItemEntity): PodcastUiState {
    val podcast = Json.decodeFromString<Podcast>(item.media)
    val downloadedIds = downloadRepo.podcastDownloadedEpisodeIds(item.title, podcast.episodes)
    val progresses = progressRepo.byLibraryItemId(item.id)

    val finished = progressRepo.byLibraryItemIdAndFinished(item.id)
    val finishedIds = finished.map { it.id }
    val finishedCount = finished.count()

    val episodeCount = podcast.episodes.count()
    val unfinishedCount = episodeCount - finishedCount

    val downloadedCount = downloadedIds.count()
    val downloadedAndFinishedCount = finishedIds.count { it in downloadedIds }
    val unfinishedAndDownloadCount = downloadedCount - downloadedAndFinishedCount

    return PodcastUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      addedAt = item.addedAt,
      progressLastUpdate = podcastProgressLastUpdate(progresses),
      episodeCount = episodeCount,
      unfinishedCount = unfinishedCount,
      downloadedCount = downloadedCount,
      unfinishedAndDownloadCount = unfinishedAndDownloadCount,
    )
  }
}

internal fun podcastProgressLastUpdate(progresses: List<ProgressEntity>): Long {
  return progresses.filter { !it.episodeId.isNullOrBlank() }.maxOfOrNull(ProgressEntity::lastUpdate)
    ?: 0L
}
