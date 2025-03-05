package dev.halim.shelfdroid.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.halim.shelfdroid.core.data.navigation.NavigationRepository
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreen
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreen
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
object Login

@Serializable
object Home

@HiltViewModel
class NavigationViewModel @Inject constructor(
    val navigationRepository: NavigationRepository
) : ViewModel()

@Composable
fun MainNavigation(
    viewModel: NavigationViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    val startDestination = if (viewModel.navigationRepository.token.isNullOrBlank()) Login else Home
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Login> {
            LoginScreen(onLoginSuccess = {
                navController.navigate(Home) { popUpTo(Login) { inclusive = true } }
            })
        }
        composable<Home> { HomeScreen {} }
    }
}
