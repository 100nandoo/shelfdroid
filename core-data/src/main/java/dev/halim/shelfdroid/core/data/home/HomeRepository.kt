package dev.halim.shelfdroid.core.data.home


import dev.halim.core.network.ApiService
import dev.halim.core.network.request.BatchLibraryItemsRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.Helper
import dev.halim.shelfdroid.core.data.ProgressRepo
import dev.halim.shelfdroid.core.database.ProgressEntity
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToLong

class HomeRepository @Inject constructor(
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager,
    private val helper: Helper,
    private val progressRepo: ProgressRepo,
) {

    private suspend fun getToken(): String = withContext(Dispatchers.IO) {
        dataStoreManager.token.first()
    }

    suspend fun getLibraries(): List<LibraryUiState> {
        val result = api.libraries()
        result.onSuccess { response ->
            return response.libraries.map { LibraryUiState(it.id, it.name) }
        }
        return emptyList()
    }

    suspend fun getLibraryItems(libraryId: String): List<ShelfdroidMediaItem> {
        val ids = api.libraryItems(libraryId).getOrNull()?.results?.map { it.id } ?: emptyList()
        val result = api.batchLibraryItems(BatchLibraryItemsRequest(ids)).getOrNull()?.libraryItems ?: emptyList()
        val progresses = progressRepo.entities().first()
        val resultWithProgress = result.map { item ->
            val progress = progresses.firstOrNull { it.libraryItemId == item.id }
            toUiState(item, progress)
        }
        return resultWithProgress
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

    private suspend fun toUiState(item: LibraryItem, progressEntity: ProgressEntity?): ShelfdroidMediaItem {
        val media = item.media
        return if (media is Book) {
            val cover = helper.generateItemCoverUrl(item.id)
            val author = media.metadata.authors.joinToString { it.name }
            val title = media.metadata.title ?: ""
            val progress = progressEntity?.progress ?: 0f
            val currentTime = progressEntity?.currentTime ?: 0f
            val (startTime, endTime) = calculateStartEndTime(media, currentTime)
            val (url, seekTime) = urlAndSeekTime(item.id, media.audioFiles.first().ino, currentTime, startTime)

            BookUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = url, seekTime = seekTime, startTime = startTime, endTime = endTime,
                progress = progress
            )
        } else {
            media as Podcast
            val cover = helper.generateItemCoverUrl(item.id)
            val author = media.metadata.author ?: ""
            val title = media.metadata.title ?: ""
            PodcastUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = "", seekTime = 0, startTime = 0, endTime = 0, episodeCount = 0
            )
        }
    }
}
