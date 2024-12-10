package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.UserEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.LibraryKey
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
        val user = getUser(fresh)
        val progresses = user.mediaProgress
        return result.asCollection().data.map { item ->
            val itemProgress = progresses.firstOrNull { item.id == it.libraryItemId }
            toUiState(item, itemProgress)
        }
    }

    private suspend fun getUser(fresh: Boolean): UserEntity {
        val userId = Holder.userId
        return if (fresh) {
            storeManager.userStore.freshOrCached(userId)
        } else {
            storeManager.userStore.cached(userId)
        }
    }

    private fun toUiState(item: ItemEntity, mediaProgress: MediaProgress?): ShelfdroidMediaItem {
        return if (item.mediaType == "book") {
            val cover = item.cover ?: ""
            val author = item.author ?: ""
            val title = item.title
            val progress = mediaProgress?.progress ?: 0f
            val currentTime = mediaProgress?.currentTime ?: 0f
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
