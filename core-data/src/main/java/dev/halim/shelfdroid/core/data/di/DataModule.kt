

package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import dev.halim.shelfdroid.core.data.AudiobookRepository
import dev.halim.shelfdroid.core.data.DefaultAudiobookRepository
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
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
