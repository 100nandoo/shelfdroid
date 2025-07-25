package dev.halim.shelfdroid.media.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.media.download.ShelfDownloadService.Companion.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import dev.halim.shelfdroid.media.download.TerminalStateNotificationHelper
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Singleton
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Module
@InstallIn(SingletonComponent::class)
object DownloadModule {

  @Provides
  @Singleton
  fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
    return StandaloneDatabaseProvider(context)
  }

  @Provides
  @Singleton
  fun provideDownloadDirectory(@ApplicationContext context: Context): File {
    val downloadDirectory = context.getExternalFilesDir(null)
    return downloadDirectory ?: context.filesDir
  }

  @Provides
  @Singleton
  fun provideCache(databaseProvider: DatabaseProvider, downloadDirectory: File): Cache {
    val cache =
      SimpleCache(File(downloadDirectory, "downloads"), NoOpCacheEvictor(), databaseProvider)

    return cache
  }

  @Provides
  @Singleton
  fun provideOkHttpDataSourceFactory(okHttpClient: OkHttpClient): DataSource.Factory {
    return OkHttpDataSource.Factory(okHttpClient)
  }

  @Provides
  @Singleton
  fun provideDownloadNotificationHelper(@ApplicationContext context: Context) =
    DownloadNotificationHelper(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)

  @Provides
  @Singleton
  fun provideDownloadManager(
    @ApplicationContext context: Context,
    databaseProvider: DatabaseProvider,
    cache: Cache,
    okhttpDataSourceFactory: DataSource.Factory,
    terminalStateNotificationHelper: TerminalStateNotificationHelper,
  ): DownloadManager {
    val downloadManager =
      DownloadManager(
        context,
        databaseProvider,
        cache,
        okhttpDataSourceFactory,
        Executors.newFixedThreadPool(6),
      )
    downloadManager.addListener(terminalStateNotificationHelper)
    return downloadManager
  }
}
