package dev.halim.shelfdroid.screen.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onLogoutSuccess: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    Text("Settings screen")
    TextButton(onClick = { viewModel.onEvent(SettingsEvent.LogoutButtonPressed) }) {
        Text("Logout")
    }

    if (uiState.settingsState == SettingsState.Success) {
        onLogoutSuccess()
    }
}