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
import androidx.navigation.serialization.generateHashCode
import dev.halim.shelfdroid.ui.screens.HomeRoute
import dev.halim.shelfdroid.ui.screens.SettingsRoute

sealed class BottomNavScreen(val id: Int, val route: Any, val title: String, val icon: ImageVector) {
    data object Settings : BottomNavScreen(
        SettingsRoute.serializer().generateHashCode(),
        SettingsRoute,
        "Settings",
        Icons.Outlined.Settings
    )

    data object Home : BottomNavScreen(
        HomeRoute.serializer().generateHashCode(),
        HomeRoute,
        "Home",
        Icons.Outlined.Home
    )
}

val BOTTOM_NAV_SCREEN = listOf(
    BottomNavScreen.Home,
    BottomNavScreen.Settings
)


@Composable
fun BottomBar(selectedRoute: Int, navigateBottomNavScreen: (BottomNavScreen) -> Unit) {
    NavigationBar(modifier = Modifier.fillMaxWidth()) {
        BOTTOM_NAV_SCREEN.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.id,
                onClick = { navigateBottomNavScreen(destination) },
                label = { Text(text = destination.title) },
                icon = {
                    Icon(imageVector = destination.icon, contentDescription = destination.title)
                })
        }
    }
}