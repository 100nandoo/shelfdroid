package dev.halim.shelfdroid.core.data.screen.book

import android.annotation.SuppressLint
import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.MultipleTrackDownloadUiState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.catalog.LibraryItemRepository
import dev.halim.shelfdroid.core.data.listening.ProgressRepository
import dev.halim.shelfdroid.core.data.prefs.PrefsRepository
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
  private val libraryItemRepo: LibraryItemRepository,
  private val progressRepo: ProgressRepository,
  private val downloadRepo: DownloadRepo,
  private val helper: Helper,
  private val prefsRepository: PrefsRepository,
) {

  @SuppressLint("UnsafeOptInUsageError")
  fun observeBook(id: String): Flow<BookUiState> {
    val bookAndMediaFlow =
      combine(libraryItemRepo.flowById(id), libraryItemRepo.flowBookById(id)) { entity, media ->
        entity?.takeIf { it.isBook.toBoolean() }?.let { it to media }
      }
    val progressFlow = progressRepo.flowBookById(id)
    val userPrefsFlow = prefsRepository.userPrefs

    return combine(
      bookAndMediaFlow,
      progressFlow,
      downloadRepo.downloads,
      downloadRepo.durableDownloads,
      userPrefsFlow,
    ) { bookAndMedia, progress, _, _, userPrefs ->
      bookAndMedia?.let { (book, media) ->
        media?.let {
          val subtitle = it.metadata.subtitle ?: ""
          val description = it.metadata.description ?: ""
          val narrator = it.metadata.narrators.joinToString()
          val publishYear = it.metadata.publishedYear ?: ""
          val publisher = it.metadata.publisher ?: ""
          val genres = it.metadata.genres.joinToString()
          val language = it.metadata.language ?: ""

          val progress = progress?.progress?.toFloat() ?: 0f
          val formattedProgress = (progress * 100).roundToInt()
          val remaining = helper.calculateRemaining(it.duration ?: 0.0, progress)

          val isEbook = it.ebookFile != null
          val canManageGeneratedRss = userPrefs.isAdmin
          val isSingleTrack = it.audioTracks.size == 1
          val generatedRssFeed =
            GeneratedRssFeedMapper.map(
              itemId = id,
              feed = book.rssFeed?.let { rssFeed -> Json.decodeFromString(rssFeed) },
              webBaseUrl = currentWebBaseUrl(),
              canManage = canManageGeneratedRss,
              hasAudioContent = it.audioTracks.isNotEmpty(),
              hasEpisodesWithoutPubDate = false,
            )

          val download =
            if (isSingleTrack) {
              downloadRepo.bookItem(
                itemId = id,
                bookTitle = book.title,
                author = book.author,
                track = it.audioTracks.first(),
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
                tracks = it.audioTracks,
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
        }
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
