package dev.halim.shelfdroid.di

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.BuildConfig
import dev.halim.shelfdroid.R
import dev.halim.shelfdroid.core.Device
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

  @Singleton
  @Provides
  @Named("version")
  fun provideVersion(): String {
    return BuildConfig.VERSION_NAME
  }

  @Singleton
  @Provides
  fun provideDevice(@ApplicationContext appContext: Context): Device {
    return Device(
      manufacturer = Build.MANUFACTURER,
      model = Build.MODEL,
      sdkVersion = Build.VERSION.SDK_INT,
      osVersion = Build.VERSION.RELEASE,
      clientName = appContext.getString(R.string.app_name),
      clientVersion = BuildConfig.VERSION_NAME,
      mediaPlayer = "media3_" + BuildConfig.MEDIA3_VERSION,
    )
  }

  @Singleton
  @Provides
  fun provideIsDebug(): Boolean {
    return BuildConfig.DEBUG
  }
}
