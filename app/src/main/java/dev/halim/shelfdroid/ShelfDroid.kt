package dev.halim.shelfdroid

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.acra.config.toastConfiguration
import org.acra.ktx.initAcra

@HiltAndroidApp
class ShelfDroid : Application(), SingletonImageLoader.Factory {

  @Inject lateinit var imageLoader: ImageLoader

  override fun attachBaseContext(base: Context) {
    super.attachBaseContext(base)
    initAcra {
      buildConfigClass = BuildConfig::class.java
      pluginConfigurations =
        listOf(
          toastConfiguration {
            text = base.getString(R.string.acra_crash_toast)
            length = android.widget.Toast.LENGTH_LONG
          }
        )
    }
  }

  override fun newImageLoader(context: PlatformContext): ImageLoader {
    return imageLoader
  }
}
