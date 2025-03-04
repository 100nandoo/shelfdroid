package dev.halim.shelfdroid.core.data.home


import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.MediaProgress
import dev.halim.core.network.response.User
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.roundToLong

class HomeRepository @Inject constructor(
    private val api: ApiService,
    private val dataStoreManager: DataStoreManager
) {
    private val token = runBlocking { dataStoreManager.token.first() }

    suspend fun getLibraries(): List<LibraryUiState> {
        val result = api.libraries()
        result.onSuccess { response ->
            return response.libraries.map { LibraryUiState(it.id, it.name) }
        }
        return emptyList()
    }

    suspend fun getLibraryItems(libraryId: String): List<ShelfdroidMediaItem> {
        val result = api.libraryItems(libraryId).getOrNull()?.results
        val user = getUser().getOrNull()
        val progresses = user?.mediaProgress
        val resultWithProgress = result?.map { item ->
            val itemProgress = progresses?.firstOrNull { it.libraryItemId == item.id }
            toUiState(item, itemProgress)
        }
        return resultWithProgress ?: emptyList()
    }

    private suspend fun getUser(): Result<User> {
        return api.me()
    }

    private inline fun getCurrentChapter(currentTime: Float, chapters: List<BookChapter>): BookChapter {
        return (chapters.find { currentTime >= it.start && currentTime <= it.end } ?: chapters.first())
    }

    private inline fun urlAndSeekTime(
        id: String,
        inoId: String,
        currentTime: Float,
        startTime: Long,
        api: ApiService
    ): Pair<String, Long> {
        val url = api.generateItemStreamUrl(DataStoreManager.BASE_URL, token, id, inoId)
        val seekTime = (currentTime * 1000).roundToLong() - startTime
        return url to seekTime
    }

    private fun toUiState(item: LibraryItem, mediaProgress: MediaProgress?): ShelfdroidMediaItem {
        val media = item.media
       return  if (media is Book) {
            val cover = api.generateItemCoverUrl(DataStoreManager.BASE_URL, token, item.id)
            val author = media.metadata.authors.joinToString { it.name }
            val title = media.metadata.title ?: ""
            val progress = mediaProgress?.progress ?: 0f
            val currentTime = mediaProgress?.currentTime ?: 0f
            val chapters = media.chapters
            val currentChapter = getCurrentChapter(currentTime, chapters)
            val startTime = (currentChapter.start * 1000).toLong()
            val endTime = (currentChapter.end * 1000).toLong()

            val (url, seekTime) = urlAndSeekTime(item.id, media.audioFiles.first().ino, currentTime, startTime, api)

           BookUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = url, seekTime = seekTime, startTime = startTime, endTime = endTime,
                progress = progress
            )
        } else {
            media as Podcast
            val cover = api.generateItemCoverUrl(DataStoreManager.BASE_URL, token, item.id)
            val author = media.metadata.author ?: ""
            val title = media.metadata.title ?: ""
            PodcastUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = "", seekTime = 0, startTime = 0, endTime = 0, episodeCount = 0
            )
        }
    }
}
