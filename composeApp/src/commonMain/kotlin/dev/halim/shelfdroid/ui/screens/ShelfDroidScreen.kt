package dev.halim.shelfdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreen
import dev.halim.shelfdroid.ui.screens.player.PlayerScreen
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
object LoginRoute

@Serializable
object HomeRoute

@Serializable
object SplashRoute

@Serializable
object SettingsRoute

@Serializable
object PlayerRoute


fun NavGraphBuilder.declareComposeScreen(
    navController: NavHostController,
    paddingValues: PaddingValues
) {

    composable<LoginRoute> {
        LoginScreen(paddingValues, onLoginSuccess = {
            navController.navigate(HomeRoute) {
                popUpTo(LoginRoute){
                    inclusive = true
                }
            }
        })
    }
    composable<HomeRoute> {
        HomeScreen(paddingValues, { bookUiState ->
            navController.navigate(bookUiState)
        })
    }
    composable<SplashRoute> {
        SplashScreen(paddingValues)
    }
    composable<SettingsRoute> {
        SettingsScreen(paddingValues, onLogoutSuccess = {
            navController.navigate(LoginRoute) {
                popUpTo(navController.graph.id) {
                    inclusive = true
                }
            }
        })
    }
    composable<BookUiState> { backStackEntry ->
        val bookUiState: BookUiState = backStackEntry.toRoute()
        PlayerScreen(paddingValues, bookUiState)
    }
}