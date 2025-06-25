package dev.halim.shelfdroid.core.ui.di

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object UiModule {

  @Singleton
  @Provides
  fun providesImageLoader(
    @ApplicationContext appContext: Context,
    okhttpClient: OkHttpClient,
  ): ImageLoader {
    return ImageLoader.Builder(appContext)
      .memoryCachePolicy(CachePolicy.ENABLED)
      .components { add(OkHttpNetworkFetcherFactory(callFactory = { okhttpClient })) }
      .memoryCache { MemoryCache.Builder().maxSizePercent(appContext, 0.25).build() }
      .diskCachePolicy(CachePolicy.ENABLED)
      .diskCache {
        DiskCache.Builder()
          .directory(appContext.cacheDir.resolve("image_cache"))
          .maxSizeBytes(100 * 1024 * 1024)
          .build()
      }
      .crossfade(true)
      .logger(DebugLogger())
      .build()
  }
}
