@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.UserType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.UserSettingsEditUserUiState
import dev.halim.shelfdroid.core.navigation.NavUsersSettingsEditUser
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSwitchItem
import kotlinx.coroutines.launch

@Composable
fun UserSettingsEditUserScreen(
  viewModel: UserSettingsEditUserViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  onUpdateSuccess: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  UserSettingsEditUserContent(uiState = uiState, onEvent = viewModel::onEvent)

  val scope = rememberCoroutineScope()
  val successMessage = stringResource(R.string.user_updated)
  val errorMessage = stringResource(R.string.update_user_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is GenericState.Success -> {
        scope.launch { snackbarHostState.showSuccessSnackbar(successMessage) }
        onUpdateSuccess(uiState.editUser.id)
      }
      is GenericState.Failure -> {
        scope.launch { snackbarHostState.showErrorSnackbar(state.errorMessage ?: errorMessage) }
      }
      else -> Unit
    }
  }
}

@Composable
private fun UserSettingsEditUserContent(
  uiState: UserSettingsEditUserUiState = UserSettingsEditUserUiState(),
  onEvent: (UserSettingsEditUserEvent) -> Unit = {},
) {
  val isNotRoot = uiState.editUser.type.isRoot().not()

  val scrollState = rememberScrollState()

  Column(Modifier.padding(horizontal = 16.dp).verticalScroll(scrollState)) {
    VisibilityDown(uiState.apiState is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    InfoSection(uiState, onEvent)

    if (isNotRoot) {
      SettingsSwitchItem(
        modifier = Modifier.padding(start = 8.dp),
        title = stringResource(R.string.enable),
        checked = uiState.editUser.isActive,
        contentDescription = stringResource(R.string.enable),
        onCheckedChange = {
          onEvent(UserSettingsEditUserEvent.Update { it.copy(isActive = it.isActive.not()) })
        },
      )

      HorizontalDivider(Modifier.padding(vertical = 16.dp))

      PermissionSection(uiState.editUser, onEvent)
    } else {
      HorizontalDivider(Modifier.padding(vertical = 16.dp))

      RootSection()
    }

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = { onEvent(UserSettingsEditUserEvent.Submit) },
      modifier = Modifier.align(Alignment.End),
    ) {
      Text(stringResource(R.string.submit))
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun InfoSection(
  uiState: UserSettingsEditUserUiState,
  onEvent: (UserSettingsEditUserEvent) -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val isNotRoot = uiState.editUser.type.isRoot().not()
  val (usernameRef, passwordRef, emailRef) = remember { FocusRequester.createRefs() }

  MyOutlinedTextField(
    modifier = Modifier.focusRequester(usernameRef),
    value = uiState.editUser.username,
    onValueChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(username = it.username.trim()) })
    },
    label = stringResource(R.string.username),
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
    onNext = { focusManager.moveFocus(FocusDirection.Next) },
  )

  Spacer(Modifier.height(12.dp))

  if (isNotRoot) {
    PasswordTextField(
      modifier = Modifier.focusRequester(passwordRef),
      value = uiState.editUser.password,
      onValueChange = {
        onEvent(UserSettingsEditUserEvent.Update { it.copy(password = it.password.trim()) })
      },
      label = stringResource(R.string.change_password),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
      onDone = { focusManager.moveFocus(FocusDirection.Next) },
    )

    Spacer(Modifier.height(12.dp))
  }

  MyOutlinedTextField(
    modifier = Modifier.focusRequester(emailRef),
    value = uiState.editUser.email,
    onValueChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(email = it.email.trim()) })
    },
    label = stringResource(R.string.email),
    keyboardOptions =
      KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    onNext = { focusManager.moveFocus(FocusDirection.Next) },
  )

  Spacer(Modifier.height(12.dp))

  if (isNotRoot) {
    val options = remember { UserType.editTypes.map { it.name } }
    MySegmentedButton(
      modifier = Modifier.fillMaxWidth(),
      options,
      stringResource(R.string.account_type),
      uiState.editUser.type.name,
      { selection ->
        onEvent(UserSettingsEditUserEvent.Update { it.copy(type = UserType.valueOf(selection)) })
      },
    )
  }
}

@Composable
private fun RootSection() {
  Button(onClick = {}) { Text(stringResource(R.string.change_root_password)) }
}

@Composable
fun PermissionSection(
  editUser: NavUsersSettingsEditUser,
  onEvent: (UserSettingsEditUserEvent) -> Unit,
) {
  TextTitleMedium(text = stringResource(R.string.permissions))
  SettingsSwitchItem(
    title = stringResource(R.string.download),
    checked = editUser.download,
    contentDescription = stringResource(R.string.download),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(download = it.download.not()) })
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.update),
    checked = editUser.update,
    contentDescription = stringResource(R.string.update),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(update = it.update.not()) })
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.delete),
    checked = editUser.delete,
    contentDescription = stringResource(R.string.delete),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(delete = it.delete.not()) })
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.upload),
    checked = editUser.upload,
    contentDescription = stringResource(R.string.upload),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(upload = it.upload.not()) })
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.create_ereader),
    checked = editUser.createEReader,
    contentDescription = stringResource(R.string.create_ereader),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(createEReader = it.createEReader.not()) })
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.access_explicit_content),
    checked = editUser.accessExplicit,
    contentDescription = stringResource(R.string.access_explicit_content),
    onCheckedChange = {
      onEvent(
        UserSettingsEditUserEvent.Update { it.copy(accessExplicit = it.accessExplicit.not()) }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.access_all_libraries),
    checked = editUser.accessAllLibraries,
    contentDescription = stringResource(R.string.access_all_libraries),
    onCheckedChange = {
      onEvent(
        UserSettingsEditUserEvent.Update {
          it.copy(accessAllLibraries = it.accessAllLibraries.not())
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.access_all_tags),
    checked = editUser.accessAllTags,
    contentDescription = stringResource(R.string.access_all_tags),
    onCheckedChange = {
      onEvent(UserSettingsEditUserEvent.Update { it.copy(accessAllTags = it.accessAllTags.not()) })
    },
  )
}

@ShelfDroidPreview
@Composable
fun UserSettingsEditUserScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { UserSettingsEditUserContent() }
}

@ShelfDroidPreview
@Composable
fun UserSettingsEditUserScreenRootUserContentPreview() {
  val editUser = NavUsersSettingsEditUser(type = UserType.Root)

  AnimatedPreviewWrapper(dynamicColor = false) {
    UserSettingsEditUserContent(uiState = UserSettingsEditUserUiState().copy(editUser = editUser))
  }
}
