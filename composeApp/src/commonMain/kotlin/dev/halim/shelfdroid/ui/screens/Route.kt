package dev.halim.shelfdroid.ui.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import dev.halim.shelfdroid.ui.screens.detail.PodcastScreen
import dev.halim.shelfdroid.ui.screens.home.HomeScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreen
import dev.halim.shelfdroid.ui.screens.player.PlayerScreen
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreen

enum class Route(val title: String) {
    Login(title = "login"),
    Home(title = "home"),
    Splash(title = "splash"),
    Settings(title = "settings"),
    Player(title = "player"),
    Podcast(title = "podcast")
}

fun NavGraphBuilder.declareComposeScreen(
    navController: NavHostController,
    paddingValues: PaddingValues
) {


    composable(Route.Login.title) {
        LoginScreen(paddingValues, onLoginSuccess = {
            navController.navigate(Route.Home.title) {
                popUpTo(Route.Login.title) {
                    inclusive = true
                }
            }
        })
    }
    composable(Route.Home.title) {
        HomeScreen(paddingValues,
            { id -> navController.navigate("${Route.Player.title}/$id") },
            { id -> navController.navigate("${Route.Podcast.title}/$id") })
    }
    composable(Route.Splash.title) {
        SplashScreen(paddingValues)
    }
    composable(Route.Settings.title) {
        SettingsScreen(paddingValues, onLogoutSuccess = {
            navController.navigate(Route.Login.title) {
                popUpTo(Route.Login.title) {
                }
            }
        })
    }
    composable(
        "${Route.Player.title}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id")
        id?.let {
            PlayerScreen(paddingValues, id)
        }
    }
    composable(
        "${Route.Podcast.title}/{id}",
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
    ) { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id")
        id?.let {
            PodcastScreen(paddingValues, id)
        }
    }
}