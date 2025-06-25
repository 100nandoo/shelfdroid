package dev.halim.shelfdroid.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.shelfdroid.BuildConfig
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
}
