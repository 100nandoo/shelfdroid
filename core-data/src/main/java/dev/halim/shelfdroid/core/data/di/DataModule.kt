

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
import dev.halim.shelfdroid.core.data.UserPrefs
import dev.halim.shelfdroid.core.data.home.HomeRepository
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Singleton
    @Provides
    fun providesHomeRepository(api: ApiService, userPrefs: UserPrefs): HomeRepository {
        return HomeRepository(api, userPrefs)
    }

    @Singleton
    @Provides
    fun providesUserPrefs(dataStoreManager: DataStoreManager): UserPrefs {
        return runBlocking {
            UserPrefs(
                dataStoreManager = dataStoreManager,
                token = dataStoreManager.token.first(),
                baseUrl = dataStoreManager.baseUrl.first(),
                deviceId = dataStoreManager.deviceId.first(),
                darkMode = dataStoreManager.darkMode.first()
            )
        }
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
