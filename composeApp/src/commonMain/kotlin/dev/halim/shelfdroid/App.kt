package dev.halim.shelfdroid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.datastore.DataStoreKeys
import dev.halim.shelfdroid.di.appModule
import dev.halim.shelfdroid.screen.MainScreen
import dev.halim.shelfdroid.screen.ShelfDroidScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.koin.compose.KoinApplication
import org.koin.compose.getKoin

@Composable
fun App() {
    KoinApplication(
        application = { modules(appModule) }
    ) {
        val navController = rememberNavController()
        val startDestination = decideStartDestination()

        MaterialTheme {
            MainScreen(navController, startDestination)
        }
    }
}

@Composable
private fun decideStartDestination(): MutableState<String> {
    val dataStore: DataStore<Preferences> = getKoin().get<DataStore<Preferences>>()
    val dataStoreKeys = getKoin().get<DataStoreKeys>()

    val startDestination = remember { mutableStateOf(ShelfDroidScreen.Splash.title) }

    LaunchedEffect(Unit) {
        val token = dataStore.data.map { preferences ->
            preferences[dataStoreKeys.TOKEN]
        }.firstOrNull()
        startDestination.value = if (token != null) {
            ShelfDroidScreen.Home.title
        } else {
            ShelfDroidScreen.Login.title
        }
    }
    return startDestination
}
