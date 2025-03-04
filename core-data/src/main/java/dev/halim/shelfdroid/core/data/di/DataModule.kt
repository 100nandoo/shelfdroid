

package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.core.network.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import dev.halim.shelfdroid.core.data.AudiobookRepository
import dev.halim.shelfdroid.core.data.DefaultAudiobookRepository
import dev.halim.shelfdroid.core.data.home.HomeRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun providesHomeRepository(api: ApiService, dataStoreManager: DataStoreManager): HomeRepository {
        return HomeRepository(api, dataStoreManager)
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface DataExampleModule {
    @Binds
    fun bindsAudiobookRepository(
        audiobookRepository: DefaultAudiobookRepository
    ): AudiobookRepository
}

class FakeAudiobookRepository @Inject constructor() : AudiobookRepository {
    override val audiobooks: Flow<List<String>> = flowOf(fakeAudiobooks)

    override suspend fun add(name: String) {
        throw NotImplementedError()
    }
}

val fakeAudiobooks = listOf("One", "Two", "Three")
