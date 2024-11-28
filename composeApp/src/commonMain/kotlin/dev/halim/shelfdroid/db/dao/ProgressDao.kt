package dev.halim.shelfdroid.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import dev.halim.shelfdroid.db.ProgressEntity
import dev.halim.shelfdroid.db.ProgressQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class ProgressDao(private val progressQueries: ProgressQueries) {
    fun upsertProgress(entity: ProgressEntity) {
        progressQueries.upsert(entity.id, entity.itemId, entity.progress, entity.currentTime)
    }

    fun addAllProgress(list: List<ProgressEntity>) {
        list.forEach { upsertProgress(it) }
    }

    fun getProgressByItemId(id: String): Flow<ProgressEntity> {
        return progressQueries.selectByItemId(id).asFlow().mapToOneNotNull(Dispatchers.IO)
    }

    fun allProgress(): Flow<List<ProgressEntity>> {
        return progressQueries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun removeProgress(id: String) {
        progressQueries.removeById(id)
    }

    fun removeAllProgress() {
        progressQueries.removeAll()
    }
}