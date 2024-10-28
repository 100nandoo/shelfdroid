package dev.halim.shelfdroid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.datastore.DataStoreManager
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.BASE_URL
import dev.halim.shelfdroid.datastore.DataStoreManager.DataStoreKeys.TOKEN
import dev.halim.shelfdroid.di.appModule
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.screen.MainScreen
import dev.halim.shelfdroid.screen.ShelfDroidScreen
import kotlinx.coroutines.flow.first
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin

@Composable
fun App() {
    KoinApplication(
        application = { modules(appModule) }
    ) {
        val navController = rememberNavController()
        val startDestination = setupInitialState()

        MaterialTheme {
            MainScreen(navController, startDestination)
        }
    }
}

@Composable
private fun setupInitialState(): MutableState<String> {
    val dataStoreManager: DataStoreManager = getKoin().get()

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
