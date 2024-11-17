package dev.halim.shelfdroid.ui.screens.settings

import SettingsBody
import SettingsLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.ui.components.SettingsSwitchItem
import dev.halim.shelfdroid.version
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(paddingValues: PaddingValues, onLogoutSuccess: () -> Unit) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        paddingValues,
        uiState,
        { settingsEvent -> viewModel.onEvent(settingsEvent) },
        onLogoutSuccess
    )
}

@Composable
fun SettingsScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    uiState: SettingsUiState = SettingsUiState(),
    onEvent: (SettingsEvent) -> Unit = {},
    onLogoutSuccess: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        SettingsLabel(text = "Display")
        Spacer(modifier = Modifier.height(4.dp))
        SettingsSwitchItem(
            title = "Dark Mode",
            checked = uiState.isDarkMode,
            onCheckedChange = { onEvent(SettingsEvent.SwitchToggle(it)) },
            contentDescription = "Dark Mode"
        )

        Spacer(modifier = Modifier.height(16.dp))

        SettingsLabel(text = "Others")
        Spacer(modifier = Modifier.height(4.dp))
        SettingsBody(text = "Version $version")

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(
            onClick = { onEvent(SettingsEvent.LogoutButtonPressed) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text("Logout")
        }

        if (uiState.settingsState == SettingsState.Success) {
            onLogoutSuccess()
        }
    }
}