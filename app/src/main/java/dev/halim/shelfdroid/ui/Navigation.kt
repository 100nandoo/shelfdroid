package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.version
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Home

@Serializable
object Settings

@Composable
fun MainNavigation(isLoggedIn: Boolean) {
    val navController = rememberNavController()

    val startDestination = if (isLoggedIn) Home else Login
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Home) { popUpTo(Login) { inclusive = true } }
            })
        }
        composable<Home> { HomeScreen {} }
        composable<Settings> { SettingsScreen(version = version) {} }
    }
}
