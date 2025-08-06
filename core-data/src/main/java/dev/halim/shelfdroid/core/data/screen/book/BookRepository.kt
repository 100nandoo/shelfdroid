package dev.halim.shelfdroid.core.data.screen.book

import android.annotation.SuppressLint
import androidx.media3.exoplayer.offline.Download
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.media.DownloadMapper
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import java.util.Locale
import javax.inject.Inject
import kotlinx.serialization.json.Json

class BookRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val downloadMapper: DownloadMapper,
  private val helper: Helper,
) {

  @SuppressLint("UnsafeOptInUsageError")
  suspend fun item(id: String): BookUiState {
    val result = libraryItemRepo.byId(id)
    val progressEntity = progressRepo.bookById(id)
    return if (result != null && result.isBook == 1L) {
      val media = Json.decodeFromString<Book>(result.media)
      val subtitle = media.metadata.subtitle ?: ""
      val description = media.metadata.description ?: ""
      val narrator = media.metadata.narrators.joinToString()
      val publishYear = media.metadata.publishedYear ?: ""
      val publisher = media.metadata.publisher ?: ""
      val genres = media.metadata.genres.joinToString()
      val language = media.metadata.language ?: ""

      val progress = progressEntity?.progress?.toFloat() ?: 0f
      val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)
      val remaining = helper.calculateRemaining(media.duration ?: 0.0, progress)

      val isSingleTrack = media.audioTracks.size == 1

      val downloadUiState =
        if (isSingleTrack) {
          val url = media.audioTracks.first().contentUrl
          downloadRepo.item(itemId = id, url = url)
        } else {
          DownloadUiState()
        }

      BookUiState(
        state = GenericState.Success,
        author = result.author,
        narrator = narrator,
        title = result.title,
        subtitle = subtitle,
        duration = result.duration,
        remaining = remaining,
        cover = result.cover,
        description = description,
        publishYear = publishYear,
        publisher = publisher,
        genres = genres,
        language = language,
        progress = formattedProgress,
        isSingleTrack = isSingleTrack,
        download = downloadUiState,
      )
    } else {
      BookUiState(state = GenericState.Failure("Failed to fetch book"))
    }
  }

  @SuppressLint("UnsafeOptInUsageError")
  fun updateDownloads(uiState: BookUiState, download: Download): BookUiState {
    val downloadUiState =
      uiState.download.copy(state = downloadMapper.toDownloadState(download.state))

    return uiState.copy(download = downloadUiState)
  }
}
