package dev.halim.shelfdroid.db.dao

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneNotNull
import dev.halim.shelfdroid.db.UserEntity
import dev.halim.shelfdroid.db.UserQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

class UserDao(private val queries: UserQueries) {
    fun upsertUser(userEntity: UserEntity) {
        queries.upsert(
            userEntity.id,
            userEntity.username,
            userEntity.mediaProgress,
            userEntity.bookmarks,
        )
    }

    fun addAllUser(list: List<UserEntity>) {
        list.forEach { upsertUser(it) }
    }

    fun getUser(id: String): Flow<UserEntity> {
        return queries.selectById(id).asFlow().mapToOneNotNull(Dispatchers.IO)
    }

    fun allUser(): Flow<List<UserEntity>> {
        return queries.selectAll().asFlow().mapToList(Dispatchers.IO)
    }

    fun removeUser(id: String) {
        queries.removeById(id)
    }

    fun removeAllUser() {
        queries.removeAll()
    }
}