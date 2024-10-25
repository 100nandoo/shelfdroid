package dev.halim.shelfdroid.screen

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import dev.halim.shelfdroid.screen.home.HomeScreen
import dev.halim.shelfdroid.screen.login.LoginScreen
import dev.halim.shelfdroid.screen.settings.SettingsScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

enum class ShelfDroidScreen(val title: String) {
    Login(title = "login"),
    Home(title = "home"),
    Splash(title = "splash"),
    Settings(title = "settings"),
}

fun NavGraphBuilder.declareComposeScreen(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope
) {

    composable(ShelfDroidScreen.Login.title) {
        LoginScreen(onLoginSuccess = {
            navController.navigate(ShelfDroidScreen.Home.title) {
                popUpTo(ShelfDroidScreen.Login.title) {
                    inclusive = true
                }
            }
        }, onLoginFailure = { errorMessage ->
            scope.launch { snackbarHostState.showSnackbar(errorMessage) }
        })
    }
    composable(ShelfDroidScreen.Home.title) {
        HomeScreen()
    }
    composable(ShelfDroidScreen.Splash.title) {
        SplashScreen()
    }
    composable(ShelfDroidScreen.Settings.title) {
        SettingsScreen()
    }
}