package dev.halim.shelfdroid.di

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.createDataStoreManager
import dev.halim.shelfdroid.screen.login.LoginViewModel
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.network.book.Book
import dev.halim.shelfdroid.network.book.Item
import dev.halim.shelfdroid.network.book.Podcast
import dev.halim.shelfdroid.screen.home.HomeViewModel
import dev.halim.shelfdroid.screen.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    viewModel { LoginViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(json = get(), contentType = ContentType.Any)
            }
        }
    }
    single<Api> { Api(get(), get()) }
    single<DataStoreManager> { createDataStoreManager() }

    single {
        Json {
            serializersModule = SerializersModule {
                polymorphic(Item::class){
                    subclass(Book::class)
                    subclass(Podcast::class)
                }
            }
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }
}