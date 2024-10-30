package dev.halim.shelfdroid.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(paddingValues: PaddingValues, onLogoutSuccess: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    val bottomPadding = paddingValues.calculateBottomPadding()

    Column(
        modifier = Modifier.fillMaxHeight().fillMaxWidth().padding(bottom = bottomPadding),
        verticalArrangement = Arrangement.Bottom
    ) {
        Text("Settings screen")
        TextButton(onClick = { viewModel.onEvent(SettingsEvent.LogoutButtonPressed) }) {
            Text("Logout")
        }

        if (uiState.settingsState == SettingsState.Success) {
            onLogoutSuccess()
        }
    }
}