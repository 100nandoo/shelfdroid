package dev.halim.shelfdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
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
data class PlayerRoute(val id: String)


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
        HomeScreen(paddingValues, { id ->
            navController.navigate(PlayerRoute(id))
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
    composable<PlayerRoute> { backStackEntry ->
        val playerRoute: PlayerRoute = backStackEntry.toRoute()
        PlayerScreen(paddingValues, playerRoute.id)
    }
}