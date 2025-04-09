package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreen
import dev.halim.shelfdroid.core.ui.screen.book.BookScreen
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsScreen
import dev.halim.shelfdroid.version
import kotlinx.serialization.Serializable

@Serializable
object Login

@Serializable
object Home

@Serializable
object Settings

@Serializable
data class Podcast(val id: String)

@Serializable
data class Book(val id: String)


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
        composable<Home> {
            HomeScreen(
                onSettingsClicked = { navController.navigate(Settings) },
                onPodcastClicked = { id -> navController.navigate(Podcast(id)) },
                onBookClicked = { id -> navController.navigate(Book(id)) }
            )
        }
        composable<Podcast> {
            PodcastScreen()
        }
        composable<Book> {
            BookScreen()
        }

        composable<Settings> {
            SettingsScreen(
                version = version,
                onLogoutSuccess = {
                    navController.navigate(Login) {
                        popUpTo(0) { inclusive = true }
                    }
                })
        }
    }
}
