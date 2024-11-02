package dev.halim.shelfdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.halim.shelfdroid.ui.screens.home.HomeScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreen
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreen

enum class ShelfDroidScreen(val title: String) {
    Login(title = "login"),
    Home(title = "home"),
    Splash(title = "splash"),
    Settings(title = "settings"),
}

fun NavGraphBuilder.declareComposeScreen(
    navController: NavHostController,
    paddingValues: PaddingValues
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
        HomeScreen(paddingValues)
    }
    composable(ShelfDroidScreen.Splash.title) {
        SplashScreen()
    }
    composable(ShelfDroidScreen.Settings.title) {
        SettingsScreen(paddingValues, onLogoutSuccess = {
            navController.navigate(ShelfDroidScreen.Login.title) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        })
    }
}