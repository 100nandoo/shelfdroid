package dev.halim.shelfdroid.core.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminContract
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminRepository
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateContract
import dev.halim.shelfdroid.core.data.task.ApiTaskApi
import dev.halim.shelfdroid.core.data.task.LibraryDataTaskCatalogSynchronizer
import dev.halim.shelfdroid.core.data.task.SystemTaskClock
import dev.halim.shelfdroid.core.data.task.TaskApi
import dev.halim.shelfdroid.core.data.task.TaskCatalogSynchronizer
import dev.halim.shelfdroid.core.data.task.TaskClock
import dev.halim.shelfdroid.core.data.task.TaskRepository
import dev.halim.shelfdroid.core.data.task.TaskRepositoryContract
import dev.halim.shelfdroid.core.data.task.TaskSocket
import dev.halim.shelfdroid.core.data.task.TaskSocketManager

@Module
@InstallIn(SingletonComponent::class)
abstract class LibraryAdminModule {

  @Binds
  abstract fun bindLibraryAdminRepository(repository: LibraryAdminRepository): LibraryAdminContract

  @Binds
  abstract fun bindLibraryAdminCreateRepository(
    repository: LibraryAdminRepository
  ): LibraryAdminCreateContract

  @Binds abstract fun bindTaskSocket(socket: TaskSocketManager): TaskSocket

  @Binds abstract fun bindTaskApi(api: ApiTaskApi): TaskApi

  @Binds
  abstract fun bindTaskCatalogSynchronizer(
    synchronizer: LibraryDataTaskCatalogSynchronizer
  ): TaskCatalogSynchronizer

  @Binds abstract fun bindTaskClock(clock: SystemTaskClock): TaskClock

  @Binds abstract fun bindTaskRepository(repository: TaskRepository): TaskRepositoryContract
}
