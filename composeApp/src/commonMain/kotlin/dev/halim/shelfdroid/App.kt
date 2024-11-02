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
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.BASE_URL
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.TOKEN
import dev.halim.shelfdroid.di.appModule
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.theme.ShelfDroidTheme
import dev.halim.shelfdroid.ui.screens.MainScreen
import dev.halim.shelfdroid.ui.screens.ShelfDroidScreen
import kotlinx.coroutines.flow.first
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin
import org.koin.core.Koin
import org.koin.core.parameter.parametersOf


@Composable
fun App() {
    KoinApplication(
        application = { modules(appModule) }
    ) {
        val navController = rememberNavController()
        val koin = getKoin()
        val startDestination = setupInitialState(koin)
        val isDarkMode by koin.get<DataStoreManager>().isDarkModeFlow.collectAsState(false)
        LaunchedEffect(isDarkMode){
            SharedObject.setDarkMode(isDarkMode)
        }

        ShelfDroidTheme(isDarkMode) {
            setSingletonImageLoaderFactory { context ->
                koin.get { parametersOf(context) }
            }
            MainScreen(navController, startDestination)
        }
    }
}

@Composable
private fun setupInitialState(koin: Koin): MutableState<String> {
    val dataStoreManager: DataStoreManager = koin.get()

    val startDestination = remember { mutableStateOf(ShelfDroidScreen.Splash.title) }

    LaunchedEffect(Unit) {
        val preferences = dataStoreManager.dataStore.data.first()
        val token = preferences[TOKEN]
        val baseUrl = preferences[BASE_URL]

        if (!baseUrl.isNullOrEmpty()) {
            Api.baseUrl = baseUrl
        }

        startDestination.value = if (token != null) {
            ShelfDroidScreen.Home.title
        } else {
            ShelfDroidScreen.Login.title
        }
    }

    return startDestination
}
