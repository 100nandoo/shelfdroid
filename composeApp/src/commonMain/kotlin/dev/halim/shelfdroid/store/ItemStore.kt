package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.model.Episode
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.LibraryItem
import dev.halim.shelfdroid.network.libraryitem.Book
import dev.halim.shelfdroid.network.libraryitem.Podcast
import dev.halim.shelfdroid.network.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.store.ItemExtensions.toEntity
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder


sealed class ItemNetwork {
    data class Single(val item: LibraryItem) : ItemNetwork()
    data class Collection(val items: List<LibraryItem>) :
        ItemNetwork()
}

sealed class ItemKey {
    data class Collection(val itemIds: List<String>) : ItemKey()
    data class Single(val itemId: String) : ItemKey()
}

object ItemExtensions {
    fun PodcastEpisode.toEpisode(): Episode {
        return Episode(
            id = id,
            libraryItemId = libraryItemId,
            ino = audioFile.ino,
            title = title,
            description = description ?: "",
            subtitle = subtitle ?: "",
            publishedAt = publishedAt ?: 0L,
            seekTime = 0L,
            progress = 0f
        )
    }


    fun LibraryItem.toEntity(api: Api): ItemEntity {
        return when (media) {
            is Book -> {
                val cover = api.generateItemCoverUrl(id)
                val inoId = media.audioFiles.first().ino
                val author = media.metadata.authors.joinToString { it.name }
                val description = media.metadata.description
                val title = media.metadata.title ?: ""
                val chapters = media.chapters
                ItemEntity(
                    this.id,
                    inoId,
                    libraryId,
                    author,
                    title,
                    cover,
                    description,
                    mediaType,
                    chapters,
                    emptyList(),
                    media.audioFiles.firstOrNull()?.duration ?: 0.0
                )
            }

            is Podcast -> {
                val author = media.metadata.author
                val title = media.metadata.title ?: ""
                val description = media.metadata.description
                val cover = api.generateItemCoverUrl(id)
                val episodes = media.episodes.map { it.toEpisode() }
                ItemEntity(this.id, "", libraryId, author, title, cover, description, mediaType, emptyList(),
                    episodes, 0.0)
            }
        }
    }

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
        return Fetcher.of { key ->
            val result = when (key) {
                is ItemKey.Collection -> {
                    val ids = key.itemIds
                    val items = api.batchLibraryItems(ids).getOrNull()?.libraryItems
                        ?: error(itemError(key.itemIds.joinToString("\n")))
                    ItemNetwork.Collection(items)
                }

                is ItemKey.Single -> {
                    val item = api.libraryItem(key.itemId).getOrNull() ?: error(itemError(key.itemId))
                    ItemNetwork.Single(item)
                }
            }
            result
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
                        val entity = output.item.toEntity(api)
                        database.itemDao.upsertItem(entity)
                    }

                    is ItemNetwork.Collection -> {
                        database.itemDao.addAllItem(output.items.map { item ->
                            item.toEntity(api)
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