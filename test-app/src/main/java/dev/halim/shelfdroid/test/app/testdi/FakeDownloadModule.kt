package dev.halim.shelfdroid.test.app.testdi

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
import dagger.hilt.testing.TestInstallIn
import dev.halim.shelfdroid.download.service.ShelfDownloadService.Companion.DOWNLOAD_NOTIFICATION_CHANNEL_ID
import dev.halim.shelfdroid.download.storage.book.BookDurableDownloadExporter
import dev.halim.shelfdroid.download.storage.podcast.PodcastDurableDownloadExporter
import dev.halim.shelfdroid.media.di.DownloadModule
import dev.halim.shelfdroid.media.download.TerminalStateNotificationHelper
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors
import javax.inject.Singleton
import okhttp3.OkHttpClient

@OptIn(UnstableApi::class)
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [DownloadModule::class])
object FakeDownloadModule {

  @Provides
  @Singleton
  fun provideDatabaseProvider(@ApplicationContext context: Context): DatabaseProvider {
    return StandaloneDatabaseProvider(context)
  }

  @Provides
  @Singleton
  fun provideFileDirectory(@ApplicationContext context: Context): File {
    return File(context.cacheDir, "media3-test-${UUID.randomUUID()}").apply { mkdirs() }
  }

  @Provides
  @Singleton
  fun provideCache(databaseProvider: DatabaseProvider, fileDir: File): Cache {
    return SimpleCache(File(fileDir, "downloads"), NoOpCacheEvictor(), databaseProvider)
  }

  @Provides
  @Singleton
  fun provideOkHttpDataSourceFactory(okHttpClient: OkHttpClient): DataSource.Factory {
    return OkHttpDataSource.Factory(okHttpClient)
  }

  @Provides
  @Singleton
  fun provideDownloadNotificationHelper(@ApplicationContext context: Context): DownloadNotificationHelper {
    return DownloadNotificationHelper(context, DOWNLOAD_NOTIFICATION_CHANNEL_ID)
  }

  @Provides
  @Singleton
  fun provideDownloadManager(
    @ApplicationContext context: Context,
    databaseProvider: DatabaseProvider,
    cache: Cache,
    okhttpDataSourceFactory: DataSource.Factory,
    terminalStateNotificationHelper: TerminalStateNotificationHelper,
    bookDurableDownloadExporter: BookDurableDownloadExporter,
    podcastDurableDownloadExporter: PodcastDurableDownloadExporter,
  ): DownloadManager {
    return DownloadManager(
        context,
        databaseProvider,
        cache,
        okhttpDataSourceFactory,
        Executors.newFixedThreadPool(2),
      )
      .also {
        it.addListener(terminalStateNotificationHelper)
        it.addListener(bookDurableDownloadExporter)
        it.addListener(podcastDurableDownloadExporter)
      }
  }
}
