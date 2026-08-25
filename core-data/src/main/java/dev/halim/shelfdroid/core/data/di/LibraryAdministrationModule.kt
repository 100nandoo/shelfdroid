package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateContract
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryAdministrationModule {

  @Binds
  abstract fun bindLibraryAdministrationRepository(
    repository: LibraryAdministrationRepository
  ): LibraryAdministrationContract

  @Binds
  abstract fun bindLibraryAdministrationCreateRepository(
    repository: LibraryAdministrationRepository
  ): LibraryAdministrationCreateContract
}
