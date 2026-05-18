package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.download.AndroidManagedDownloadEnqueuer
import dev.halim.shelfdroid.core.data.download.ManagedDownloadEnqueuer
import dev.halim.shelfdroid.core.data.screen.edititem.HelperLibraryFileDownloadUrlResolver
import dev.halim.shelfdroid.core.data.screen.edititem.LibraryFileDownloadUrlResolver

@Module
@InstallIn(SingletonComponent::class)
abstract class ManagedDownloadModule {

  @Binds
  abstract fun bindManagedDownloadEnqueuer(
    enqueuer: AndroidManagedDownloadEnqueuer
  ): ManagedDownloadEnqueuer

  @Binds
  abstract fun bindLibraryFileDownloadUrlResolver(
    resolver: HelperLibraryFileDownloadUrlResolver
  ): LibraryFileDownloadUrlResolver
}
