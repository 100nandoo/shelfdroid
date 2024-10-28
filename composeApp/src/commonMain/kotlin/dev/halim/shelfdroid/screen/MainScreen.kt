package dev.halim.shelfdroid.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import dev.halim.shelfdroid.BottomBar
import dev.halim.shelfdroid.BottomNavScreen

@Composable
fun MainScreen(
    navController: NavHostController,
    startDestination: MutableState<String>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = navBackStackEntry?.destination?.route ?: BottomNavScreen.Home.route

    Scaffold(bottomBar = {
        if (navBackStackEntry?.destination?.route == BottomNavScreen.Home.route ||
            navBackStackEntry?.destination?.route == BottomNavScreen.Settings.route
        ) {
            BottomBar(selectedRoute) {
                navController.navigate(it.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    },
        modifier = Modifier.windowInsetsPadding(insets = WindowInsets.systemBars)
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination.value
        ) {
            declareComposeScreen(navController)
        }
    }
}
