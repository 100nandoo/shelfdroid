package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.ProgressKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.screens.player.BookPlayerUiState
import kotlin.math.roundToLong

class PlayerRepository(private val storeManager: StoreManager, private val api: Api) {
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

    suspend fun getPlayerModel(itemId: String): BookPlayerUiState {
        val progressEntity = storeManager.progressStore.freshOrCached(ProgressKey.Single(itemId)).asSingle().data
        val itemEntity = storeManager.itemStore.cached(ItemKey.Single(itemId)).asSingle().data

        val cover = itemEntity.cover ?: ""
        val author = itemEntity.author ?: ""
        val title = itemEntity.title
        val progress = progressEntity.progress.toFloat()
        val currentTime = progressEntity.currentTime.toFloat()
        val chapters = itemEntity.chapters
        val currentChapter = getCurrentChapter(currentTime, chapters)
        val startTime = (currentChapter.start * 1000).toLong()
        val endTime = (currentChapter.end * 1000).toLong()

        val (url, seekTime) = urlAndSeekTime(itemId, itemEntity.inoId, currentTime, startTime, api)

        return BookPlayerUiState(
            id = itemId, author = author, title = title,
            cover = cover, url = url, seekTime = seekTime, startTime = startTime, endTime = endTime,
            progress = progress, chapters = chapters, currentChapter = currentChapter
        )
    }
}