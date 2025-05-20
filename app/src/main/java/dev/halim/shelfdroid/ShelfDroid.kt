package dev.halim.shelfdroid

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class ShelfDroid : Application(), SingletonImageLoader.Factory {

  @Inject lateinit var imageLoader: ImageLoader

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    return imageLoader
  }
}

val version = BuildConfig.VERSION_NAME
