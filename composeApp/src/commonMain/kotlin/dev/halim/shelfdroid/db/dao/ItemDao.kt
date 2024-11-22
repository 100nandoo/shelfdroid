package dev.halim.shelfdroid.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import dev.halim.shelfdroid.db.ItemEntity
import dev.halim.shelfdroid.db.ItemQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class ItemDao(private val itemQuery: ItemQueries) {
    fun addItem(itemEntity: ItemEntity) {
        itemQuery.insert(
            itemEntity.id,
            itemEntity.ino,
            itemEntity.libraryId,
            itemEntity.author,
            itemEntity.title,
            itemEntity.cover,
            itemEntity.mediaType,
            itemEntity.url,
            itemEntity.progress,
            itemEntity.seekTime
        )
    }

    fun upsertItem(itemEntity: ItemEntity) {
        itemQuery.upsert(
            itemEntity.id,
            itemEntity.ino,
            itemEntity.libraryId,
            itemEntity.author,
            itemEntity.title,
            itemEntity.cover,
            itemEntity.mediaType,
            itemEntity.url,
            itemEntity.progress,
            itemEntity.seekTime
        )
    }

    fun addAllItem(list: List<ItemEntity>) {
        list.forEach { upsertItem(it) }
    }

    fun getItem(id: String): Flow<ItemEntity> {
        return itemQuery.selectById(id).asFlow().mapToOneNotNull(Dispatchers.IO)
    }

    fun allItem(): Flow<List<ItemEntity>> {
        return itemQuery.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun removeItem(id: String) {
        itemQuery.removeById(id)
    }

    fun removeAllItem() {
        itemQuery.removeAll()
    }
}