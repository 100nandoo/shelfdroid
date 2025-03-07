package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.data.UserPrefs
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Home

@Composable
fun MainNavigation(
    userPrefs: UserPrefs
) {
    val navController = rememberNavController()

    val startDestination = if (userPrefs.token.isBlank()) Login else Home
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Home) { popUpTo(Login) { inclusive = true } }
            })
        }
        composable<Home> { HomeScreen {} }
    }
}
