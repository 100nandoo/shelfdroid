package dev.halim.shelfdroid.core.ui.screen.settingsnotification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.SLEEP_TIMER_PRESET_MINUTES
import dev.halim.shelfdroid.core.data.screen.settingsnotification.SettingsNotificationUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsNotificationScreen(viewModel: SettingsNotificationViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsNotificationContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
private fun SettingsNotificationContent(
  uiState: SettingsNotificationUiState = SettingsNotificationUiState(),
  onEvent: (SettingsNotificationEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SleepTimerSection(uiState, onEvent)
  }
}

@Composable
private fun SleepTimerSection(
  uiState: SettingsNotificationUiState,
  onEvent: (SettingsNotificationEvent) -> Unit,
) {
  TextTitleMedium(text = stringResource(R.string.sleep_timer))
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
    label = stringResource(R.string.default_sleep_timer),
    options = SLEEP_TIMER_PRESET_MINUTES.map { it.toString() },
    initialValue = uiState.sleepTimerMinutes.toString(),
    onClick = { selected ->
      selected.toIntOrNull()?.let { onEvent(SettingsNotificationEvent.ChangeSleepTimerMinutes(it)) }
    },
  )
}

@ShelfDroidPreview
@Composable
fun SettingsNotificationContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsNotificationContent() }
}
