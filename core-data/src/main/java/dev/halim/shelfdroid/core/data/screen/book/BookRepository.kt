package dev.halim.shelfdroid.core.data.screen.book

import android.annotation.SuppressLint
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedDetails
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedMapper
import dev.halim.shelfdroid.core.extensions.toBoolean
import dev.halim.shelfdroid.download.DownloadRepo
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json

class BookRepository
@Inject
constructor(
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
  private val prefsRepository: PrefsRepository,
) {

  @SuppressLint("UnsafeOptInUsageError")
  fun item(id: String): Flow<BookUiState> {
    val bookFlow = libraryItemRepo.flowById(id)
    val progressFlow = progressRepo.flowBookById(id)
    val userPrefsFlow = prefsRepository.userPrefs

    return combine(
      bookFlow,
      progressFlow,
      downloadRepo.downloads,
      downloadRepo.durableDownloads,
      userPrefsFlow,
    ) { book, progress, _, _, userPrefs ->
      book
        ?.takeIf { it.isBook.toBoolean() }
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
          val formattedProgress = (progress * 100).roundToInt()
          val remaining = helper.calculateRemaining(media.duration ?: 0.0, progress)

          val isEbook = media.ebookFile != null
          val canManageGeneratedRss = userPrefs.isAdmin
          val isSingleTrack = media.audioTracks.size == 1
          val generatedRssFeed =
            GeneratedRssFeedMapper.map(
              itemId = id,
              feed = it.rssFeed?.let { rssFeed -> Json.decodeFromString(rssFeed) },
              webBaseUrl = currentWebBaseUrl(),
              canManage = canManageGeneratedRss,
              hasAudioContent = media.audioTracks.isNotEmpty(),
              hasEpisodesWithoutPubDate = false,
            )

          val download =
            if (isSingleTrack) {
              downloadRepo.bookItem(
                itemId = id,
                bookTitle = book.title,
                author = book.author,
                track = media.audioTracks.first(),
              )
            } else {
              DownloadUiState()
            }

          val downloads =
            if (isSingleTrack.not()) {
              downloadRepo.multipleTrackItem(
                itemId = id,
                title = book.title,
                author = book.author,
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
            isEbook = isEbook,
            isSingleTrack = isSingleTrack,
            canEdit = userPrefs.update,
            generatedRssFeed = generatedRssFeed,
            download = download,
            downloads = downloads,
          )
        } ?: BookUiState(state = GenericState.Failure("Failed to fetch book"))
    }
  }

  suspend fun openGeneratedRssFeed(
    itemId: String,
    details: GeneratedRssFeedDetails,
  ): Result<Unit> {
    return libraryItemRepo.openGeneratedRssFeedForItem(itemId = itemId, details = details).map {}
  }

  suspend fun closeGeneratedRssFeed(itemId: String, feedId: String): Result<Unit> {
    return libraryItemRepo.closeGeneratedRssFeedForItem(itemId = itemId, feedId = feedId)
  }

  private fun currentWebBaseUrl(): String = libraryItemRepo.currentWebBaseUrlForUi()
}
