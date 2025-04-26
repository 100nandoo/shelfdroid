package dev.halim.shelfdroid.core.ui.screen.settings

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.halim.shelfdroid.core.ui.components.SettingsBody
import dev.halim.shelfdroid.core.ui.components.SettingsLabel
import dev.halim.shelfdroid.core.ui.components.SettingsSwitchItem

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel(), version: String, onLogoutSuccess: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScreenContent(
        uiState,
        version,
        { settingsEvent -> viewModel.onEvent(settingsEvent) },
        onLogoutSuccess
    )
}

@Composable
fun SettingsScreenContent(
    uiState: SettingsUiState = SettingsUiState(),
    version: String = "0.2",
    onEvent: (SettingsEvent) -> Unit = {},
    onLogoutSuccess: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .systemBarsPadding()
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        SettingsLabel(text = "Display")
        Spacer(modifier = Modifier.height(4.dp))
        SettingsSwitchItem(
            title = "Dark Mode",
            checked = uiState.isDarkMode,
            onCheckedChange = { onEvent(SettingsEvent.SwitchDarkTheme(it)) },
            contentDescription = "Dark Mode"
        )
        SettingsSwitchItem(
            title = "Dynamic Theme",
            checked = uiState.isDynamicTheme,
            onCheckedChange = { onEvent(SettingsEvent.SwitchDynamicTheme(it)) },
            contentDescription = "Dynamic Theme",
            enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
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