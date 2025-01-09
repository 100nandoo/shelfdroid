package dev.halim.shelfdroid.repo

import dev.halim.shelfdroid.Holder
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.AudioBookmark
import dev.halim.shelfdroid.network.libraryitem.BookChapter
import dev.halim.shelfdroid.store.ItemKey
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.store.asSingle
import dev.halim.shelfdroid.store.cached
import dev.halim.shelfdroid.store.freshOrCached
import dev.halim.shelfdroid.ui.screens.player.BookPlayerUiState
import kotlin.math.roundToLong

class PlayerRepository(private val storeManager: StoreManager, private val api: Api, private val repoHelper: RepoHelper) {
    private inline fun getBookmarks(itemId: String, bookmarks: List<AudioBookmark>): List<AudioBookmark> =
        bookmarks.filter { it.libraryItemId == itemId }

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
        val userId = Holder.userId
        val user = storeManager.userStore.freshOrCached(userId)
        val progressEntity = user.mediaProgress.firstOrNull { it.libraryItemId == itemId }
        val itemEntity = storeManager.itemStore.cached(ItemKey.Single(itemId)).asSingle().data

        val cover = itemEntity.cover ?: ""
        val author = itemEntity.author ?: ""
        val title = itemEntity.title
        val progress = progressEntity?.progress ?: 0f
        val currentTime = progressEntity?.currentTime ?: 0f
        val chapters = itemEntity.chapters
        val currentChapter =
            if (chapters.isNotEmpty()) repoHelper.getCurrentChapter(currentTime, chapters) else null
        val startTime =  (currentChapter?.start?.times(1000))?.toLong() ?: 0
        val endTime = (currentChapter?.end?.times(1000))?.toLong() ?: itemEntity.duration.toLong()

        val (url, seekTime) = urlAndSeekTime(itemId, itemEntity.inoId, currentTime, startTime, api)
        val bookmarks = getBookmarks(itemId, user.bookmarks)
        return BookPlayerUiState(
            id = itemId, author = author, title = title,
            cover = cover, url = url, seekTime = seekTime, startTime = startTime, endTime = endTime,
            progress = progress, chapters = chapters, currentChapter = currentChapter, bookmarks = bookmarks
        )
    }
}
