package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.UserEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.User
import dev.halim.shelfdroid.store.UserExtensions.toEntity
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder


object UserExtensions {
    fun User.toEntity(): UserEntity {
        return UserEntity(id, username, mediaProgress, bookmarks)
    }
}

typealias UserStore = Store<String, UserEntity>

class UserStoreFactory(
    private val api: Api,
    private val database: Database,
) {
    fun create(): UserStore {
        return StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()
    }

    private fun createFetcher(): Fetcher<String, User> {
        return Fetcher.of {
            val result = api.me().getOrNull() ?: error(progressError())
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<String, User, UserEntity> {
        return SourceOfTruth.of(
            reader = { key -> database.userDao.getUser(key) },
            writer = { _, output ->
                val entity = output.toEntity()
                database.userDao.upsertUser(entity)
            },
            delete = { key ->
                database.userDao.removeUser(key)
            },
            deleteAll = {
                database.userDao.removeAllUser()
            }
        )
    }
}