

package dev.halim.shelfdroid.test.app.testdi

import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.halim.shelfdroid.core.data.AudiobookRepository
import dev.halim.shelfdroid.core.data.di.DataExampleModule
import dev.halim.shelfdroid.core.data.di.FakeAudiobookRepository

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DataExampleModule::class]
)
interface FakeDataModule {

    @Binds
    abstract fun bindRepository(
        fakeRepository: FakeAudiobookRepository
    ): AudiobookRepository
}
