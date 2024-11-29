package dev.halim.shelfdroid

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import dev.halim.shelfdroid.ui.screens.Route

sealed class BottomNavScreen(val route: String, val title: String, val icon: ImageVector) {
    data object Settings : BottomNavScreen(
        Route.Settings.title,
        Route.Settings.title,
        Icons.Outlined.Settings
    )

    data object Home : BottomNavScreen(
        Route.Home.title,
        Route.Home.title,
        Icons.Outlined.Home
    )
}

val BOTTOM_NAV_SCREEN = listOf(
    BottomNavScreen.Home,
    BottomNavScreen.Settings
)


@Composable
fun BottomBar(selectedRoute: String, navigateBottomNavScreen: (BottomNavScreen) -> Unit) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        BOTTOM_NAV_SCREEN.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { navigateBottomNavScreen(destination) },
                label = { Text(text = destination.title) },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = destination.title)
                })
        }
    }
}