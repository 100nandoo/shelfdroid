@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsApiState
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.USER_SETTINGS_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun UserSettingsScreen(
  viewModel: UserSettingsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  onUserClicked: (NavEditUser) -> Unit = {},
  createUserClicked: () -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  UserSettingsContent(
    uiState = uiState,
    onEvent = viewModel::onEvent,
    onUserClicked,
    createUserClicked,
  )

  val scope = rememberCoroutineScope()
  val successMessage = stringResource(R.string.user_deleted)
  val errorMessage = stringResource(R.string.delete_user_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is UserSettingsApiState.DeleteSuccess -> {
        scope.launch { snackbarHostState.showSuccessSnackbar(successMessage) }
      }
      is UserSettingsApiState.DeleteFailure -> {
        scope.launch { snackbarHostState.showErrorSnackbar(state.message ?: errorMessage) }
      }
      else -> Unit
    }
  }
}

@Composable
private fun UserSettingsContent(
  uiState: UserSettingsUiState = UserSettingsUiState(),
  onEvent: (UserSettingsEvent) -> Unit = {},
  onUserClicked: (NavEditUser) -> Unit = {},
  createUserClicked: () -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is UserSettingsApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    LazyColumn(
      Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      item(key = "add user") { CreateUserItem(createUserClicked = createUserClicked) }
      items(uiState.users, key = { it.id }) { user ->
        HorizontalDivider()

        UserSettingsItem(
          user,
          isLoginUserRoot = uiState.isLoginUserRoot,
          onInfoClicked = { onEvent(UserSettingsEvent.UserInfo(user)) },
          onDeleteClicked = { onEvent(UserSettingsEvent.DeleteUser(user)) },
          onClicked = { onUserClicked(user.navPayload) },
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
fun CreateUserItem(createUserClicked: () -> Unit) {
  TextButton(onClick = createUserClicked, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
    Text(text = stringResource(R.string.add_user))
  }
}

@ShelfDroidPreview
@Composable
fun UserSettingsScreenContentPreview() {
  val uiState = USER_SETTINGS_UI_STATE
  AnimatedPreviewWrapper(dynamicColor = false) { UserSettingsContent(uiState = uiState) }
}
