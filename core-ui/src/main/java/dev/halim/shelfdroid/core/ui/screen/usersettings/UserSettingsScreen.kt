package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun UserSettingsScreen(
  viewModel: UserSettingsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  UserSettingsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun UserSettingsContent(
  uiState: UserSettingsUiState = UserSettingsUiState(),
  onEvent: (UserSettingsEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Text("UserSettingsScreen")
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun UserSettingsScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { UserSettingsContent() }
}
