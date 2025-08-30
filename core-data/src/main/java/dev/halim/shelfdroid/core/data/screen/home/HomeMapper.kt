package dev.halim.shelfdroid.core.data.screen.home

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.download.DownloadRepo
import javax.inject.Inject
import kotlinx.serialization.json.Json

@SuppressLint("UnsafeOptInUsageError")
class HomeMapper
@Inject
constructor(private val progressRepo: ProgressRepo, private val downloadRepo: DownloadRepo) {

  fun toBookUiState(item: LibraryItemEntity): BookUiState {
    val trackIndexes = Json.decodeFromString<Book>(item.media).audioTracks.map { it.index }
    val isDownloaded = downloadRepo.isBookDownloaded(item.id, trackIndexes)
    return BookUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      isDownloaded = isDownloaded,
      trackIndexes = trackIndexes,
    )
  }

  fun toPodcastUiState(item: LibraryItemEntity): PodcastUiState {
    val downloadedIds = downloadRepo.podcastDownloadedEpisodeIds(item.id)

    val finished = progressRepo.byLibraryItemIdAndFinished(item.id)
    val finishedIds = finished.map { it.id }
    val finishedCount = finished.count()

    val episodeCount = Json.decodeFromString<Podcast>(item.media).episodes.count()
    val unfinishedCount = episodeCount - finishedCount

    val downloadedCount = downloadedIds.count()
    val downloadedAndFinishedCount = finishedIds.count { it in downloadedIds }
    val unfinishedAndDownloadCount = downloadedCount - downloadedAndFinishedCount

    return PodcastUiState(
      id = item.id,
      author = item.author,
      title = item.title,
      cover = item.cover,
      episodeCount = episodeCount,
      unfinishedCount = unfinishedCount,
      downloadedCount = downloadedCount,
      unfinishedAndDownloadCount = unfinishedAndDownloadCount,
    )
  }
}
