package dev.halim.shelfdroid

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.login.LoginScreen
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        LoginScreen()
    }
}