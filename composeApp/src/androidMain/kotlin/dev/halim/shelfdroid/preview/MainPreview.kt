package dev.halim.shelfdroid.preview

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import dev.halim.shelfdroid.theme.ShelfDroidTheme
import dev.halim.shelfdroid.ui.screens.SplashScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreenContent
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreenContent

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoginScreenPreview() {
    ShelfDroidTheme(false) {
        LoginScreenContent()
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DarkLoginScreenPreview() {
    ShelfDroidTheme(true) {
        LoginScreenContent()
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    ShelfDroidTheme(false) {
        Scaffold { paddingValues ->
            SettingsScreenContent(paddingValues)
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DarkSettingsScreenPreview() {
    ShelfDroidTheme(true) {
        Scaffold { paddingValues ->
            SettingsScreenContent(paddingValues)
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SplashScreenPreview() {
    ShelfDroidTheme(false) {
        Scaffold { paddingValues ->
            SplashScreen(paddingValues)
        }
    }
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun DarkSplashScreenPreview() {
    ShelfDroidTheme(true) {
        Scaffold { paddingValues ->
            SplashScreen(paddingValues)
        }
    }
}
