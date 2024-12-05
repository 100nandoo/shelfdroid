package dev.halim.shelfdroid

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import coil3.compose.setSingletonImageLoaderFactory
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.theme.ShelfDroidTheme
import dev.halim.shelfdroid.ui.screens.MainScreen
import dev.halim.shelfdroid.ui.screens.Route
import kotlinx.coroutines.flow.firstOrNull
import org.koin.compose.KoinContext
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf


@Composable
fun App() {
    KoinContext {
        val navController = rememberNavController()
        val startDestination = setupInitialState()
        val koin = getKoin()
        val dataStoreManager: DataStoreManager = koinInject()
        val isDarkMode by dataStoreManager.isDarkMode.collectAsState(true)

        ShelfDroidTheme(isDarkMode) {
            setSingletonImageLoaderFactory { context ->
                koin.get { parametersOf(context) }
            }
            MainScreen(navController, startDestination)
        }
    }
}

@Composable
private fun setupInitialState(): MutableState<String> {
    val dataStoreManager: DataStoreManager = koinInject()

    val startDestination = remember { mutableStateOf(Route.Splash.title) }

    LaunchedEffect(Unit) {
        val token = dataStoreManager.token.firstOrNull()
        val baseUrl = dataStoreManager.baseUrl.firstOrNull()

        if (!baseUrl.isNullOrBlank()) {
            Api.baseUrl = baseUrl
        }

        startDestination.value = if (token.isNullOrBlank().not()) {
            Route.Home.title
        } else {
            Route.Login.title
        }
    }

    return startDestination
}

val version = "0.1.0 build 05-12-2024"
val app_name = "Shelfdroid"