package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.ProgressEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.LibraryKey
import dev.halim.shelfdroid.store.ProgressKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asCollection
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.LibraryUiState
import dev.halim.shelfdroid.ui.screens.home.PodcastUiState
import kotlin.math.roundToLong

class HomeRepository(private val storeManager: StoreManager, private val api: Api) {
    private inline fun getCurrentChapter(currentTime: Float, chapters: List<BookChapter>): BookChapter {
        return (chapters.find { currentTime >= it.start && currentTime <= it.end } ?: chapters.first())
    }

    private inline fun urlAndSeekTime(
        id: String,
        inoId: String,
        currentTime: Float,
        startTime: Long,
        api: Api
    ): Pair<String, Long> {
        val url = api.generateItemStreamUrl(id, inoId)
        val seekTime = (currentTime * 1000).roundToLong() - startTime
        return url to seekTime
    }

    suspend fun getLibraries(fresh: Boolean): List<LibraryUiState> {
        val result =
            if (fresh) storeManager.libraryStore.freshOrCached(LibraryKey.All)
            else storeManager.libraryStore.cached(LibraryKey.All)
        val librariesUiState = result.asCollection().data.map { LibraryUiState(it.id, it.name) }
        return librariesUiState
    }

    suspend fun getLibraryItems(libraryId: String, fresh: Boolean): List<ShelfdroidMediaItem> {
        val itemIds = storeManager.libraryStore.cached(LibraryKey.Single(libraryId)).asSingle().data.itemIds
        val itemKey = ItemKey.Collection(itemIds)
        val result =
            if (fresh) storeManager.itemStore.freshOrCached(itemKey)
            else storeManager.itemStore.cached(itemKey)
        val progresses = getMediaProgresses(fresh)
        return result.asCollection().data.map { item ->
            if (item.mediaType == "book"){

            } else {

            }
            toUiState(item, progresses.firstOrNull {
                item.id == it.itemId
            })
        }
    }

    private suspend fun getMediaProgresses(fresh: Boolean): List<ProgressEntity> {
        val result =
            if (fresh) storeManager.progressStore.freshOrCached(ProgressKey.All) else
                storeManager.progressStore.cached(ProgressKey.All)
        return result.asCollection().data
    }

    private fun toUiState(item: ItemEntity, progressEntity: ProgressEntity?): ShelfdroidMediaItem {
        return if (item.mediaType == "book") {
            val cover = item.cover ?: ""
            val author = item.author ?: ""
            val title = item.title
            val progress = progressEntity?.progress?.toFloat() ?: 0f
            val currentTime = progressEntity?.currentTime?.toFloat() ?: 0f
            val chapters = item.chapters
            val currentChapter = getCurrentChapter(currentTime, chapters)
            val startTime = (currentChapter.start * 1000).toLong()
            val endTime = (currentChapter.end * 1000).toLong()

            val (url, seekTime) = urlAndSeekTime(item.id, item.inoId, currentTime, startTime, api)

            BookUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = url, seekTime = seekTime, startTime = startTime, endTime = endTime,
                progress = progress, chapters = chapters
            )
        } else {
            val cover = item.cover ?: ""
            val author = item.author ?: ""
            val title = item.title
            PodcastUiState(
                id = item.id, author = author, title = title,
                cover = cover, url = "", seekTime = 0, startTime = 0, endTime = 0, episodeCount = 0
            )
        }
    }
}