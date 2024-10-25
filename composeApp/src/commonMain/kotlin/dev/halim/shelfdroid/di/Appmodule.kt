package dev.halim.shelfdroid.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.halim.shelfdroid.datastore.DataStoreKeys
import dev.halim.shelfdroid.datastore.createDataStore
import dev.halim.shelfdroid.screen.login.LoginViewModel
import dev.halim.shelfdroid.network.Api
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    viewModel { LoginViewModel(get(), get(), get()) }
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(json = get(), contentType = ContentType.Any)
            }
        }
    }
    single<Api> { Api(get()) }
    single<DataStore<Preferences>> { createDataStore() }
    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }
    single<DataStoreKeys>{
        DataStoreKeys()
    }
}