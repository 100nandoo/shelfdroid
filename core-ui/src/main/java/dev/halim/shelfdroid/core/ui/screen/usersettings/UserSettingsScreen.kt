@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.USER_SETTINGS_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun UserSettingsScreen(
  viewModel: UserSettingsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  onUserClicked: (NavUsersSettingsEditUser) -> Unit = {},
  result: String? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LaunchedEffect(result) {
    if (result != null) {
      viewModel.onEvent(UserSettingsEvent.UpdateUser(result))
    }
  }

  UserSettingsContent(uiState = uiState, onEvent = viewModel::onEvent, onUserClicked)
}

@Composable
private fun UserSettingsContent(
  uiState: UserSettingsUiState = UserSettingsUiState(),
  onEvent: (UserSettingsEvent) -> Unit = {},
  onUserClicked: (NavUsersSettingsEditUser) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    LazyColumn(
      Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      items(uiState.users, key = { it.id }) { user ->
        HorizontalDivider()

        UserSettingsItem(
          user,
          onInfoClicked = { onEvent(UserSettingsEvent.UserInfo(user)) },
          onDeleteClicked = { onEvent(UserSettingsEvent.DeleteUser(user)) },
          onClicked = { onUserClicked(user.navPayload) },
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun UserSettingsScreenContentPreview() {
  val uiState = USER_SETTINGS_UI_STATE
  AnimatedPreviewWrapper(dynamicColor = false) { UserSettingsContent(uiState = uiState) }
}
