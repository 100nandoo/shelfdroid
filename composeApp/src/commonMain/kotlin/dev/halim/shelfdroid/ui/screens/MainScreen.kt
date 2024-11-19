package dev.halim.shelfdroid.ui.screens

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.serialization.generateHashCode
import dev.halim.shelfdroid.BottomBar
import kotlin.reflect.KClass

@Composable
fun MainScreen(
    navController: NavHostController,
    startDestination: MutableState<KClass<*>>
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedRoute = navBackStackEntry?.destination?.id ?: HomeRoute.serializer().generateHashCode()
    Scaffold(bottomBar = {
        if (navBackStackEntry?.destination?.id == HomeRoute.serializer().generateHashCode() ||
            navBackStackEntry?.destination?.id == SettingsRoute.serializer().generateHashCode()
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
