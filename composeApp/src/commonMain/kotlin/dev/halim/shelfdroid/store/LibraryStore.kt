package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.LibraryEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.Library
import dev.halim.shelfdroid.store.LibraryExtensions.toEntity
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.FetcherResult
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder

object LibraryExtensions {
    fun Library.toEntity(): LibraryEntity =
        LibraryEntity(
            this.id, this.name, this.displayOrder.toLong(), this.icon, this.mediaType,
            this.provider, this.createdAt, this.lastUpdate
        )
}

typealias LibraryInput = StoreData<Library>
typealias LibraryOutput = StoreData<LibraryEntity>
typealias LibraryStore = Store<String, LibraryOutput>

class LibraryStoreFactory(
    private val api: Api,
    private val database: Database,
) {
    fun create(): LibraryStore {
        return StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()
    }

    private fun createFetcher(): Fetcher<String, LibraryInput> {
        return Fetcher.ofResult { id ->
            try {
                val result = if (id.isBlank()) {
                    api.libraries().getOrNull()?.libraries?.let { StoreData.Collection(it) }
                } else {
                    api.library(id).getOrNull()?.library?.let { StoreData.Single(it) }
                }
                result?.let { FetcherResult.Data(it) } ?: FetcherResult.Error.Message("No Library")
            } catch (e: Exception) {
                FetcherResult.Error.Exception(e)
            }
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<String, LibraryInput, LibraryOutput> {
        return SourceOfTruth.of(
            reader = { id ->
                if (id.isBlank()) {
                    database.libraries().map { entities ->
                        if (entities.isEmpty()) null else StoreData.Collection(entities)
                    }
                } else {
                    database.get(id).map { library ->
                        StoreData.Single(library)
                    }
                }
            },
            writer = { _, output ->
                when (output) {
                    is StoreData.Single -> database.add(output.data.toEntity())
                    is StoreData.Collection -> database.addAll(output.data.map { it.toEntity() })
                }
            },
            delete = { id ->
                database.remove(id)
            },
            deleteAll = {
                database.removeAll()
            }
        )
    }
}