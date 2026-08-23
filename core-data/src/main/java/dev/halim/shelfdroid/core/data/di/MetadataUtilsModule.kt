package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsContract
import dev.halim.shelfdroid.core.data.metadata.MetadataUtilsRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class MetadataUtilsModule {

  @Binds
  abstract fun bindMetadataUtilsRepository(
    repository: MetadataUtilsRepository
  ): MetadataUtilsContract
}
