package dev.halim.core.network.di

import com.skydoves.retrofit.adapters.result.ResultCallAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.halim.core.network.ApiService
import dev.halim.core.network.client.HostSelectionInterceptor
import dev.halim.core.network.client.TokenAuthenticator
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

  @Singleton
  @Provides
  fun providesJson(): Json {
    return Json {
      coerceInputValues = true
      ignoreUnknownKeys = true
      isLenient = true
      prettyPrint = true
    }
  }

  @Singleton
  @Provides
  fun providesOkHttpClient(
    dataStoreManager: DataStoreManager,
    tokenAuthenticator: TokenAuthenticator,
  ): OkHttpClient {
    return OkHttpClient.Builder()
      .addInterceptor(HostSelectionInterceptor(dataStoreManager))
      .authenticator(tokenAuthenticator)
      .build()
  }

  @Singleton
  @Provides
  fun providesRetrofit(json: Json, okHttpClient: OkHttpClient): Retrofit {
    return Retrofit.Builder()
      .baseUrl("https://www.audiobookshelf.org")
      .client(okHttpClient)
      .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
      .addCallAdapterFactory(ResultCallAdapterFactory.create())
      .build()
  }

  @Singleton
  @Provides
  fun providesApiService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)
  }
}
