package dev.halim.shelfdroid.store

import dev.halim.shelfdroid.db.Database
import dev.halim.shelfdroid.db.ProgressEntity
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.MediaProgress
import dev.halim.shelfdroid.store.ProgressExtensions.toEntity
import kotlinx.coroutines.flow.map
import org.mobilenativefoundation.store.store5.Fetcher
import org.mobilenativefoundation.store.store5.SourceOfTruth
import org.mobilenativefoundation.store.store5.Store
import org.mobilenativefoundation.store.store5.StoreBuilder


sealed class ProgressNetwork {
    data class Single(val progress: MediaProgress) : ProgressNetwork()
    data class All(val progresses: List<MediaProgress>) : ProgressNetwork()
}

sealed class ProgressKey {
    data object All : ProgressKey()
    data class Single(val itemId: String) : ProgressKey()
}

object ProgressExtensions {
    fun MediaProgress.toEntity(): ProgressEntity {
        return ProgressEntity(id, libraryItemId, progress.toDouble(), currentTime.toLong())
    }
}

typealias ProgressOutput = StoreOutput<ProgressEntity>
typealias ProgressStore = Store<ProgressKey, ProgressOutput>

class ProgressStoreFactory(
    private val api: Api,
    private val database: Database,
) {
    fun create(): ProgressStore {
        return StoreBuilder.from(createFetcher(), createSourceOfTruth()).build()
    }

    private fun createFetcher(): Fetcher<ProgressKey, ProgressNetwork> {
        return Fetcher.of { key ->
            val result = when (key) {
                is ProgressKey.All -> {
                    val mediaProgresses = api.me().getOrNull()?.mediaProgress ?: error(progressError())
                    ProgressNetwork.All(mediaProgresses)
                }

                is ProgressKey.Single -> {
                    val mediaProgress = api.me().getOrNull()?.mediaProgress?.firstOrNull { it.libraryItemId == key.itemId }
                        ?: error(progressError(key.itemId))
                    ProgressNetwork.Single(mediaProgress)
                }
            }
            result
        }
    }

    private fun createSourceOfTruth(): SourceOfTruth<ProgressKey, ProgressNetwork, ProgressOutput> {
        return SourceOfTruth.of(
            reader = { key ->
                when (key) {
                    is ProgressKey.All -> {
                        database.progressDao.allProgress()
                            .map { entities ->
                                entities.takeIf { it.isNotEmpty() }
                                    ?.let { StoreOutput.Collection(it) }
                            }
                    }

                    is ProgressKey.Single -> {
                        database.progressDao.getProgressByItemId(key.itemId).map { entity -> StoreOutput.Single(entity) }
                    }
                }
            },
            writer = { _, output ->
                when (output) {
                    is ProgressNetwork.Single -> {
                        val entity = output.progress.toEntity()
                        database.progressDao.upsertProgress(entity)
                    }

                    is ProgressNetwork.All -> {
                        val entities = output.progresses.map { it.toEntity() }
                        database.progressDao.addAllProgress(entities)
                    }
                }
            },
            delete = { key ->
                when (key) {
                    is ProgressKey.All -> database.progressDao.removeAllProgress()
                    is ProgressKey.Single -> database.progressDao.removeProgress(key.itemId)
                }
            },
            deleteAll = {
                database.progressDao.removeAllProgress()
            }
        )
    }
}