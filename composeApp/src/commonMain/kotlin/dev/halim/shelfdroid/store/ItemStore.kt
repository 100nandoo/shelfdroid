package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LibraryItem
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.network.libraryitem.Book
import dev.halim.shelfdroid.network.libraryitem.Podcast
import dev.halim.shelfdroid.store.ItemExtensions.toEntity
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder
import kotlin.math.roundToLong


sealed class ItemNetwork {
    data class Single(val item: LibraryItem, val mediaProgress: MediaProgress?) : ItemNetwork()
    data class Collection(val items: List<LibraryItem>, val mediaProgresses: List<MediaProgress>) :
        ItemNetwork()
}

sealed class ItemKey {
    data class Collection(val itemIds: List<String>) : ItemKey()
    data class Single(val itemId: String) : ItemKey()
}

object ItemExtensions {
    private inline fun findInoIdAndSeekTiming(
        id: String,
        inoDurations: Map<String, Double>,
        currentTime: Float,
        api: Api
    ): Pair<String, Long> {
        var url = api.generateItemStreamUrl(id, inoDurations.keys.first())

        if (inoDurations.size == 1) {
            val seekTime = (currentTime * 1000).roundToLong()
            return url to seekTime
        }

        var cumulativeTime = 0.0
        for ((inoId, duration) in inoDurations) {
            cumulativeTime += duration

            if (currentTime <= cumulativeTime) {
                url = api.generateItemStreamUrl(id, inoId)
                val seekTime = (currentTime - (cumulativeTime - duration)).roundToLong()
                return url to seekTime
            }
        }

        return url to 0L
    }

    fun LibraryItem.toEntity(api: Api, mediaProgress: MediaProgress?): ItemEntity {
        return when (media) {
            is Book -> {
                val cover = api.generateItemCoverUrl(id)
                val inoDurations = media.audioFiles.associate { it.ino to it.duration }
                val author = media.metadata.authors.joinToString { it.name }
                val title = media.metadata.title ?: ""
                val progress = mediaProgress?.progress?.toDouble() ?: 0.0
                val currentTime = mediaProgress?.currentTime ?: 0f
                val (url, seekTime) = findInoIdAndSeekTiming(id, inoDurations, currentTime, api)
                ItemEntity(
                    this.id,
                    this.ino,
                    this.libraryId,
                    author,
                    title,
                    cover,
                    this.mediaType,
                    url,
                    progress,
                    seekTime
                )
            }

            is Podcast -> {
                // TODO("Handle url, progress, seekTime")
                val author = media.metadata.author
                val title = media.metadata.title ?: ""
                val cover = api.generateItemCoverUrl(id)
                ItemEntity(this.id, this.ino, this.libraryId, author, title, cover, this.mediaType, "", 0.0, 0)
            }
        }
    }

    fun ItemEntity.toBookUiState(): BookUiState {
        return BookUiState(
            this.id, this.author ?: "", this.title, this.cover ?: "", this.progress.toFloat(),
            this.url ?: "", this.seekTime
        )
    }
    // TODO: handle to PodcastUiState

}

typealias ItemOutput = StoreOutput<ItemEntity>
typealias ItemStore = Store<ItemKey, ItemOutput>

class ItemStoreFactory(
    private val api: Api,
    private val database: Database,
) {
    fun create(): ItemStore {
        return StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()
    }

    private fun createFetcher(): Fetcher<ItemKey, ItemNetwork> {
        return Fetcher.ofResult { key ->
            try {
                val result = when (key) {
                    is ItemKey.Collection -> {
                        val ids = key.itemIds
                        val items = api.batchLibraryItems(ids).getOrNull()?.libraryItems
                        val mediaProgresses = api.me().getOrNull()?.mediaProgress ?: emptyList()
                        items?.let { ItemNetwork.Collection(it, mediaProgresses) }
                    }

                    is ItemKey.Single -> {
                        val item = api.libraryItem(key.itemId).getOrNull()
                        val mediaProgress = api.me().getOrNull()?.mediaProgress
                            ?.firstOrNull { it.libraryItemId == key.itemId }
                        if (item != null) {
                            ItemNetwork.Single(item, mediaProgress)
                        } else {
                            null
                        }
                    }
                }
                result?.let { FetcherResult.Data(it) } ?: FetcherResult.Error.Message("No Item")
            } catch (e: Exception) {
                FetcherResult.Error.Exception(e)
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<ItemKey, ItemNetwork, ItemOutput> {
        return SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    is ItemKey.Collection -> {
                        database.itemDao.allItem()
                            .map { entities ->
                                entities.takeIf { it.isNotEmpty() }
                                    ?.filter { it.id in key.itemIds }
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let { StoreOutput.Collection(it) }
                            }
                    }
                    is ItemKey.Single -> {
                        database.itemDao.getItem(key.itemId).map { item -> StoreOutput.Single(item) }
                    }
                }
            },
            writer = { _, output ->
                when (output) {
                    is ItemNetwork.Single -> {
                        val entity = output.item.toEntity(api, output.mediaProgress)
                        database.itemDao.upsertItem(entity)
                    }

                    is ItemNetwork.Collection -> {
                        database.itemDao.addAllItem(output.items.map { item ->
                            val mediaProgress = output.mediaProgresses.firstOrNull { it.libraryItemId == item.id }
                            item.toEntity(api, mediaProgress)
                        })
                    }
                }
            },
            delete = { key ->
                when (key) {
                    is ItemKey.Collection -> database.itemDao.removeAllItem()
                    is ItemKey.Single -> database.itemDao.removeItem(key.itemId)
                }
            },
            deleteAll = {
                database.itemDao.removeAllItem()
            }
        )
    }
}