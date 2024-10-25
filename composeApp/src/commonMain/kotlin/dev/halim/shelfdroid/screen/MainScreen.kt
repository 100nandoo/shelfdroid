package dev.halim.shelfdroid.screen

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = navBackStackEntry?.destination?.route ?: BottomNavScreen.Home.route

    Scaffold(bottomBar = {
        BottomBar(selectedRoute) {
            navController.navigate(it.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }) {
        NavHost(
            navController = navController,
            startDestination = startDestination.value
        ) {
            declareComposeScreen(navController, snackbarHostState, scope)
        }
    }
}
