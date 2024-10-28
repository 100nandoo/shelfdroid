package dev.halim.shelfdroid.screen

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.halim.shelfdroid.screen.home.HomeScreen
import dev.halim.shelfdroid.screen.login.LoginScreen
import dev.halim.shelfdroid.screen.settings.SettingsScreen

enum class ShelfDroidScreen(val title: String) {
    Login(title = "login"),
    Home(title = "home"),
    Splash(title = "splash"),
    Settings(title = "settings"),
}

fun NavGraphBuilder.declareComposeScreen(
    navController: NavHostController
) {

    composable(ShelfDroidScreen.Login.title) {
        LoginScreen(onLoginSuccess = {
            navController.navigate(ShelfDroidScreen.Home.title) {
                popUpTo(ShelfDroidScreen.Login.title) {
                    inclusive = true
                }
            }
        })
    }
    composable(ShelfDroidScreen.Home.title) {
        HomeScreen()
    }
    composable(ShelfDroidScreen.Splash.title) {
        SplashScreen()
    }
    composable(ShelfDroidScreen.Settings.title) {
        SettingsScreen(onLogoutSuccess = {
            navController.navigate(ShelfDroidScreen.Login.title) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        })
    }
}