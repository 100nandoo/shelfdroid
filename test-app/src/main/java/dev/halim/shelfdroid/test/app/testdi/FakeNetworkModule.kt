package dev.halim.shelfdroid.test.app.testdi

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import dev.halim.core.network.ApiService
import dev.halim.core.network.di.NetworkModule
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [NetworkModule::class])
object FakeNetworkModule {

  @Provides
  @Singleton
  fun provideJson(): Json {
    return Json {
      coerceInputValues = true
      ignoreUnknownKeys = true
      isLenient = true
      prettyPrint = true
      explicitNulls = false
    }
  }

  @Provides
  @Singleton
  fun provideOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder().build()
  }

  @Provides
  @Singleton
  fun provideFakeApiService(): FakeApiService = FakeApiService()

  @Provides
  @Singleton
  fun provideApiService(fakeApiService: FakeApiService): ApiService = fakeApiService
}
