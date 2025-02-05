package dev.halim.shelfdroid.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.crossfade
import coil3.util.DebugLogger
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.createDataStore
import dev.halim.shelfdroid.db.DatabaseAdapter
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.expect.targetModule
import dev.halim.shelfdroid.mapper.Mapper
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.player.SessionManager
import dev.halim.shelfdroid.player.Timer
import dev.halim.shelfdroid.repo.EpisodeRepository
import dev.halim.shelfdroid.repo.HomeRepository
import dev.halim.shelfdroid.repo.PlayerRepository
import dev.halim.shelfdroid.repo.PodcastRepository
import dev.halim.shelfdroid.repo.RepoHelper
import dev.halim.shelfdroid.store.StoreManager
import dev.halim.shelfdroid.ui.screens.episode.EpisodeViewModel
import dev.halim.shelfdroid.ui.screens.podcast.PodcastViewModel
import dev.halim.shelfdroid.ui.screens.home.HomeViewModel
import dev.halim.shelfdroid.ui.screens.login.LoginViewModel
import dev.halim.shelfdroid.ui.screens.player.PlayerViewModel
import dev.halim.shelfdroid.ui.screens.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import okio.FileSystem
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module

object ComponentName {
    const val IO = "io"
    const val MAIN = "main"
}


private val appModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(json = get(), contentType = ContentType.Any)
            }
        }
    }
    single<CoroutineScope> {
        CoroutineScope(Dispatchers.Default)
    }
    singleOf(::Api)
    single<DataStoreManager> { DataStoreManager(get(), get(), get(named(ComponentName.IO))) }
    single<DataStore<Preferences>> { createDataStore(get()) }
    singleOf(::SessionManager)
    single<MediaManager> { MediaManager(get(), get(), get(), get(), get(named(ComponentName.MAIN))) }

    single {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
            isLenient = true
        }
    }

    single { (context: PlatformContext) ->
        val httpClient: HttpClient = get()

        ImageLoader.Builder(context)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .components { add(KtorNetworkFetcherFactory(httpClient)) }
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, 0.25)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "image_cache")
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger()).build()
    }

    single<CoroutineScope>(named(ComponentName.IO)) { CoroutineScope(Dispatchers.IO + SupervisorJob()) }
    single<CoroutineScope>(named(ComponentName.MAIN)) { CoroutineScope(Dispatchers.Main + SupervisorJob()) }

    factory { StoreManager(get(), get(), get(named(ComponentName.IO))) }
    single<Timer> { Timer(get(named(ComponentName.MAIN))) }
    singleOf(::DatabaseAdapter)
    singleOf(::Mapper)
}

private val viewModelModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::HomeViewModel)
    viewModelOf(::PlayerViewModel)
    viewModel { (id: String) ->
        PlayerViewModel(get(), get(), id)
    }
    viewModel { (id: String) ->
        PodcastViewModel(get(),get(), id)
    }
    viewModel { (id: String, episodeId: String) ->
        EpisodeViewModel(get(), id, episodeId)
    }
}

private val repositoryModule = module {
    singleOf(::RepoHelper)
    singleOf(::PlayerRepository)
    singleOf(::HomeRepository)
    singleOf(::PodcastRepository)
    singleOf(::EpisodeRepository)
}


val allModules = listOf(targetModule, appModule, repositoryModule, viewModelModule)
