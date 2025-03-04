package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Home

@Composable
fun MainNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Login) {
        composable<Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Home) { popUpTo(Login) { inclusive = true } }
            })
        }
        composable<Home> { HomeScreen {} }
    }
}
