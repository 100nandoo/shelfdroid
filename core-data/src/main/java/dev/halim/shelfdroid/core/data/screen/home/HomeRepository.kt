package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.response.BookmarkRepo
import dev.halim.shelfdroid.core.data.response.LibraryItemRepo
import dev.halim.shelfdroid.core.data.response.LibraryRepo
import dev.halim.shelfdroid.core.data.response.ProgressRepo
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class HomeRepository
@Inject
constructor(
  private val json: Json,
  private val dataStoreManager: DataStoreManager,
  private val helper: Helper,
  private val api: ApiService,
  private val libraryItemRepo: LibraryItemRepo,
  private val progressRepo: ProgressRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val libraryRepo: LibraryRepo,
) {

  private suspend fun getToken(): String =
    withContext(Dispatchers.IO) { dataStoreManager.token.first() }

  suspend fun getUser(uiState: HomeUiState): HomeUiState {
    val response = api.me()
    val result = response.getOrNull()

    return if (result != null) {
      progressRepo.saveAndConvert(result)
      bookmarkRepo.saveAndConvert(result)
      uiState.copy(homeState = HomeState.Success)
    } else {
      uiState.copy(homeState = HomeState.Failure("Get User Failed"))
    }
  }

  suspend fun getLibraries(): List<LibraryUiState> {
    val result = libraryRepo.entities()
    return result.map { LibraryUiState(it.id, it.name) }
  }

  suspend fun getLibraryItems(libraryId: String): List<ShelfdroidMediaItem> {
    val ids = libraryItemRepo.idsByLibraryId(libraryId)
    val progresses = progressRepo.entities()
    val libraryItems = libraryItemRepo.entities(libraryId, ids)
    return libraryItems.map { item ->
      val progress = progresses.firstOrNull { it.libraryItemId == item.id }
      toUiState(item, progress)
    }
  }

  private fun calculateStartEndTime(book: Book, currentTime: Float): Pair<Long, Long> {
    val chapters = book.chapters
    val startTime: Long
    val endTime: Long
    if (chapters.isEmpty()) {
      startTime = currentTime.toLong()
      endTime = book.audioFiles.firstOrNull()?.duration?.toLong() ?: 0
    } else {
      val currentChapter = getCurrentChapter(currentTime, chapters)
      startTime = (currentChapter.start * 1000).toLong()
      endTime = (currentChapter.end * 1000).toLong()
    }

    return startTime to endTime
  }

  private fun getCurrentChapter(currentTime: Float, chapters: List<BookChapter>): BookChapter {
    return (chapters.find { currentTime >= it.start && currentTime <= it.end } ?: chapters.first())
  }

  private suspend fun urlAndSeekTime(
    id: String,
    inoId: String,
    currentTime: Float,
    startTime: Long,
  ): Pair<String, Long> {
    val url = helper.generateItemStreamUrl(getToken(), id, inoId)
    val seekTime = (currentTime * 1000).roundToLong() - startTime
    return url to seekTime
  }

  private suspend fun toUiState(
    item: LibraryItemEntity,
    progress: ProgressEntity?,
  ): ShelfdroidMediaItem {
    return if (item.isBook == 1L) {
      val media = json.decodeFromString<Book>(item.media)
      val progressValue = progress?.progress?.toFloat() ?: 0f
      val currentTime = progress?.currentTime?.toFloat() ?: 0f
      val (startTime, endTime) = calculateStartEndTime(media, currentTime)
      val (url, seekTime) =
        urlAndSeekTime(item.id, media.audioFiles.first().ino, currentTime, startTime)

      BookUiState(
        id = item.id,
        author = item.author,
        title = item.title,
        cover = item.cover,
        url = url,
        seekTime = seekTime,
        startTime = startTime,
        endTime = endTime,
        progress = progressValue,
      )
    } else {
      val media = json.decodeFromString<Podcast>(item.media)
      PodcastUiState(
        id = item.id,
        author = item.author,
        title = item.title,
        cover = item.cover,
        url = "",
        seekTime = 0,
        startTime = 0,
        endTime = 0,
        episodeCount = 0,
      )
    }
  }
}
