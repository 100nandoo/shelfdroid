package dev.halim.shelfdroid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.di.appModule
import dev.halim.shelfdroid.login.LoginScreen
import org.koin.compose.KoinApplication

@Composable
fun App() {
    KoinApplication(
        application = { modules(appModule) }
    ) {
        val navController = rememberNavController()
        MaterialTheme {
            NavHost(
                navController = navController,
                startDestination = ShelfDroidScreen.Login.title
            ) {
                composable(ShelfDroidScreen.Login.title) {
                    LoginScreen()
                }
            }
        }
    }
}