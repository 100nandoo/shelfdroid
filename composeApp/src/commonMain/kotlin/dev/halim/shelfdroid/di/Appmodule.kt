package dev.halim.shelfdroid.di

import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.createDataStoreManager
import dev.halim.shelfdroid.screen.login.LoginViewModel
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.screen.home.HomeViewModel
import dev.halim.shelfdroid.screen.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
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
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }
}