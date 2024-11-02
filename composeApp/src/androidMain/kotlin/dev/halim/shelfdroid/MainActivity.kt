package dev.halim.shelfdroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import dev.halim.shelfdroid.ui.screens.SplashScreen
import dev.halim.shelfdroid.ui.screens.home.HomeScreen
import dev.halim.shelfdroid.ui.screens.login.LoginScreen
import dev.halim.shelfdroid.ui.screens.settings.SettingsScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextUtils.context = this
        enableEdgeToEdge()
        val controller = WindowCompat.getInsetsController(window, window.decorView)

        setContent {
            val isDarkMode = SharedObject.isDarkMode.collectAsState()
            controller.isAppearanceLightStatusBars = isDarkMode.value.not()
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen({})
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen(PaddingValues())
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen()
}

@Preview(showSystemUi = true, showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen(PaddingValues()) {}
}