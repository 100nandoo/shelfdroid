package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminRepository
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateContract
import dev.halim.shelfdroid.core.data.task.ApiServerTaskApi
import dev.halim.shelfdroid.core.data.task.LibraryDataServerTaskCatalogSynchronizer
import dev.halim.shelfdroid.core.data.task.ServerTaskApi
import dev.halim.shelfdroid.core.data.task.ServerTaskCatalogSynchronizer
import dev.halim.shelfdroid.core.data.task.ServerTaskClock
import dev.halim.shelfdroid.core.data.task.ServerTaskRepository
import dev.halim.shelfdroid.core.data.task.ServerTaskRepositoryContract
import dev.halim.shelfdroid.core.data.task.ServerTaskSocket
import dev.halim.shelfdroid.core.data.task.SocketManagerServerTaskSocket
import dev.halim.shelfdroid.core.data.task.SystemServerTaskClock

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryAdminModule {

  @Binds
  abstract fun bindLibraryAdminRepository(repository: LibraryAdminRepository): LibraryAdminContract

  @Binds
  abstract fun bindLibraryAdminCreateRepository(
    repository: LibraryAdminRepository
  ): LibraryAdminCreateContract

  @Binds abstract fun bindServerTaskSocket(socket: SocketManagerServerTaskSocket): ServerTaskSocket

  @Binds abstract fun bindServerTaskApi(api: ApiServerTaskApi): ServerTaskApi

  @Binds
  abstract fun bindServerTaskCatalogSynchronizer(
    synchronizer: LibraryDataServerTaskCatalogSynchronizer
  ): ServerTaskCatalogSynchronizer

  @Binds abstract fun bindServerTaskClock(clock: SystemServerTaskClock): ServerTaskClock

  @Binds
  abstract fun bindServerTaskRepository(
    repository: ServerTaskRepository
  ): ServerTaskRepositoryContract
}
