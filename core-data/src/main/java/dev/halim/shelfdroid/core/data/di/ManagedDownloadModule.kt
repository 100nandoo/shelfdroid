package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.download.AndroidManagedDownloadEnqueuer
import dev.halim.shelfdroid.core.data.download.ManagedDownloadEnqueuer

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagedDownloadModule {

  @Binds
  abstract fun bindManagedDownloadEnqueuer(
    enqueuer: AndroidManagedDownloadEnqueuer
  ): ManagedDownloadEnqueuer
}
