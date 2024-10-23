package dev.halim.shelfdroid.di

import dev.halim.shelfdroid.login.LoginViewModel
import dev.halim.shelfdroid.network.Api
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module


val appModule = module {
    viewModel { LoginViewModel(get()) }
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(json = Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                }, contentType = ContentType.Any)
            }
        }
    }
    single<Api> { Api(get()) }

}