package dev.halim.shelfdroid.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
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
    val selectedRoute = navBackStackEntry?.destination?.route ?: Route.Home.title

    Scaffold(
        bottomBar = {
            val routesWithBottomBar = listOf(BottomNavScreen.Home.route, BottomNavScreen.Settings.route)
            val isBottomBarVisible = navBackStackEntry?.destination?.route in routesWithBottomBar

            AnimatedVisibility(
                isBottomBarVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
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
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination.value
        ) {
            declareComposeScreen(navController, paddingValues)
        }
    }
}
