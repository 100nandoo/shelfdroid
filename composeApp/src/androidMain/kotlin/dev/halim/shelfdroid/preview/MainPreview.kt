package dev.halim.shelfdroid.preview

import ShelfDroidPreview
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.ui.screens.SplashScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreenContent
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreenContent

@ShelfDroidPreview
@Composable
fun LoginScreenPreview() {
    ShelfDroidPreview() {
        LoginScreenContent()
    }
}


@ShelfDroidPreview
@Composable
fun SettingsScreenPreview() {
    ShelfDroidPreview { paddingValues ->
        SettingsScreenContent(paddingValues)
    }
}


@ShelfDroidPreview
@Composable
fun SplashScreenPreview() {
    ShelfDroidPreview { paddingValues ->
        SplashScreen(paddingValues)
    }
}
