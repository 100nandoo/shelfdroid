package dev.halim.shelfdroid.core.data.screen.book

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.media.DownloadRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.helper.Helper
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BookRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
) {

  @SuppressLint("UnsafeOptInUsageError")
  fun item(id: String): Flow<BookUiState> {
    val bookFlow = libraryItemRepo.flowById(id)
    val progressFlow = progressRepo.flowBookById(id)
    val download =
      downloadRepo.downloads.map { downloads -> downloads.find { it.request.id == id } }

    return combine(bookFlow, progressFlow, download) { book, progress, _ ->
      book
        ?.takeIf { it.isBook == 1L }
        ?.let {
          val media = Json.decodeFromString<Book>(book.media)
          val subtitle = media.metadata.subtitle ?: ""
          val description = media.metadata.description ?: ""
          val narrator = media.metadata.narrators.joinToString()
          val publishYear = media.metadata.publishedYear ?: ""
          val publisher = media.metadata.publisher ?: ""
          val genres = media.metadata.genres.joinToString()
          val language = media.metadata.language ?: ""

          val progress = progress?.progress?.toFloat() ?: 0f
          val formattedProgress = String.format(Locale.getDefault(), "%.0f", progress * 100)
          val remaining = helper.calculateRemaining(media.duration ?: 0.0, progress)

          val isSingleTrack = media.audioTracks.size == 1

          val download =
            if (isSingleTrack) {
              val url = media.audioTracks.first().contentUrl
              downloadRepo.item(itemId = id, url = url, title = book.title)
            } else {
              DownloadUiState()
            }

          val downloads =
            if (isSingleTrack.not()) {
              downloadRepo.multipleTrackItem(
                itemId = id,
                title = book.title,
                tracks = media.audioTracks,
              )
            } else {
              MultipleTrackDownloadUiState()
            }

          BookUiState(
            state = GenericState.Success,
            author = book.author,
            narrator = narrator,
            title = book.title,
            subtitle = subtitle,
            duration = book.duration,
            remaining = remaining,
            cover = book.cover,
            description = description,
            publishYear = publishYear,
            publisher = publisher,
            genres = genres,
            language = language,
            progress = formattedProgress,
            isSingleTrack = isSingleTrack,
            download = download,
            downloads = downloads,
          )
        } ?: BookUiState(state = GenericState.Failure("Failed to fetch book"))
    }
  }
}
