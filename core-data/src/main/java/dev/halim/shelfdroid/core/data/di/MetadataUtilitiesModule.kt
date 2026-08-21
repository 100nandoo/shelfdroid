package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepository
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilitiesRepositoryContract

@Module
@InstallIn(SingletonComponent::class)
abstract class MetadataUtilitiesModule {

  @Binds
  abstract fun bindMetadataUtilitiesRepository(
    repository: MetadataUtilitiesRepository
  ): MetadataUtilitiesRepositoryContract
}
