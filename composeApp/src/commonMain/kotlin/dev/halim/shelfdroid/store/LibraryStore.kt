package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.LibraryEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.Library
import dev.halim.shelfdroid.store.LibraryExtensions.toEntity
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder


sealed class LibraryNetwork {
    data class Single(val library: Library, val itemIds: List<String>) : LibraryNetwork()
    data class Collection(val libraries: List<Library>, val itemIds: Map<String, List<String>>) : LibraryNetwork()
}

sealed class LibraryKey {
    data object All : LibraryKey()
    data class Single(val id: String) : LibraryKey()
}

object LibraryExtensions {
    fun Library.toEntity(ids: List<String>): LibraryEntity =
        LibraryEntity(
            this.id, this.name, this.displayOrder.toLong(), this.icon, this.mediaType,
            this.provider, this.createdAt, this.lastUpdate, ids
        )
}

typealias LibraryOutput = StoreOutput<LibraryEntity>
typealias LibraryStore = Store<LibraryKey, LibraryOutput>

class LibraryStoreFactory(
    private val api: Api,
    private val database: Database,
) {
    fun create(): LibraryStore {
        return StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()
    }

    private fun createFetcher(): Fetcher<LibraryKey, LibraryNetwork> {
        return Fetcher.of { key ->
            val result = when (key) {
                is LibraryKey.All -> {
                    val ids = mutableMapOf<String, List<String>>()
                    val libraries = api.libraries().getOrNull()?.libraries ?: error(libraryError())
                    libraries.forEach {
                        val result = api.libraryItems(it.id)
                        result.onSuccess { response ->
                            ids[it.id] = response.results.map { it.id }
                        }
                        result.onFailure { error ->
                            error(error)
                        }
                    }
                    LibraryNetwork.Collection(libraries, ids)
                }

                is LibraryKey.Single -> {
                    val library = api.library(key.id).getOrNull()?.library ?: error(libraryError(key.id))
                    val result = api.libraryItems(library.id)
                    val ids = mutableListOf<String>()
                    result.onSuccess { response ->
                        ids.addAll(response.results.map { it.id })
                    }
                    result.onFailure { error ->
                        error(error)
                    }
                    LibraryNetwork.Single(library, ids)
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<LibraryKey, LibraryNetwork, LibraryOutput> {
        return SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    is LibraryKey.All -> {
                        database.libraryDao.allLibrary()
                            .map { entities -> if (entities.isEmpty()) null else StoreOutput.Collection(entities) }
                    }

                    is LibraryKey.Single -> {
                        database.libraryDao.getLibrary(key.id).map { library -> StoreOutput.Single(library) }
                    }
                }
            },
            writer = { _, output ->
                when (output) {
                    is LibraryNetwork.Single -> {
                        database.libraryDao.upsertLibrary(output.library.toEntity(output.itemIds))
                    }

                    is LibraryNetwork.Collection -> database.libraryDao.addAllLibrary(output.libraries.map {
                        it.toEntity(
                            output.itemIds[it.id] ?: emptyList()
                        )
                    })
                }
            },
            delete = { key ->
                when (key) {
                    is LibraryKey.All -> database.libraryDao.removeAllLibrary()
                    is LibraryKey.Single -> database.libraryDao.removeLibrary(key.id)
                }
            },
            deleteAll = {
                database.libraryDao.removeAllLibrary()
            }
        )
    }
}

