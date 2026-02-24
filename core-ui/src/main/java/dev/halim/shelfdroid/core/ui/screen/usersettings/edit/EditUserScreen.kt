@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.usersettings.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
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
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserState
import dev.halim.shelfdroid.core.data.screen.usersettings.edit.EditUserUiState
import dev.halim.shelfdroid.core.navigation.NavEditUser
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DropdownOutlinedTextField
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
fun EditUserScreen(
  viewModel: EditUserViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  EditUserContent(uiState = uiState, onEvent = viewModel::onEvent)

  SnackbarHandling(viewModel, snackbarHostState, navigateBack)
}

@Composable
private fun EditUserContent(
  uiState: EditUserUiState = EditUserUiState(),
  onEvent: (UserSettingsEditUserEvent) -> Unit = {},
) {
  val isNotRoot = uiState.editUser.type.isRoot().not()

  val scrollState = rememberScrollState()

  Column(
    Modifier.padding(horizontal = 16.dp).verticalScroll(scrollState),
    verticalArrangement = Arrangement.Bottom,
  ) {
    VisibilityDown(uiState.state is EditUserState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    InfoSection(uiState, onEvent)

    if (isNotRoot) {
      NonRootSection(uiState, onEvent)
    } else {
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
private fun SnackbarHandling(
  viewModel: EditUserViewModel,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
) {
  val successMessage = stringResource(R.string.user_updated)
  val createSuccessMessage = stringResource(R.string.user_created)
  val errorMessage = stringResource(R.string.update_user_failed)
  val createErrorMessage = stringResource(R.string.create_user_failed)
  val tagsErrorMessage = stringResource(R.string.please_select_at_least_one_tag)
  val librariesErrorMessage = stringResource(R.string.please_select_at_least_one_library)
  val bothErrorMessage = stringResource(R.string.please_select_at_least_one)
  val usernameError = stringResource(R.string.username_cannot_be_empty)
  val passwordError = stringResource(R.string.password_cannot_be_empty)
  val usernamePasswordError = stringResource(R.string.username_and_password_cannot_be_empty)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is GenericUiEvent.ShowErrorSnackbar -> {
          val message =
            when (viewModel.uiState.value.state) {
              is EditUserState.LibrariesAndItemTagsFieldError -> bothErrorMessage
              is EditUserState.ItemTagsFieldError -> tagsErrorMessage
              is EditUserState.LibrariesFieldError -> librariesErrorMessage

              is EditUserState.UsernameAndPasswordFieldError -> usernamePasswordError
              is EditUserState.UsernameFieldError -> usernameError
              is EditUserState.PasswordFieldError -> passwordError

              is EditUserState.ApiUpdateError -> errorMessage
              is EditUserState.ApiCreateError -> createErrorMessage
              else -> null
            }
          launch { message?.let { snackbarHostState.showErrorSnackbar(it) } }
        }

        is GenericUiEvent.ShowSuccessSnackbar -> {
          val message =
            when (viewModel.uiState.value.state) {
              is EditUserState.ApiUpdateSuccess -> successMessage
              is EditUserState.ApiCreateSuccess -> createSuccessMessage
              else -> null
            }
          launch { message?.let { snackbarHostState.showSuccessSnackbar(it) } }
        }

        GenericUiEvent.NavigateBack -> navigateBack()
        else -> Unit
      }
    }
  }
}

@Composable
private fun InfoSection(uiState: EditUserUiState, onEvent: (UserSettingsEditUserEvent) -> Unit) {
  val (usernameRef, passwordRef, emailRef) = remember { FocusRequester.createRefs() }

  val needFocus = uiState.editUser.isCreateMode() && uiState.state == EditUserState.Success

  LaunchedEffect(needFocus) {
    if (needFocus) {
      usernameRef.requestFocus()
    }
  }

  val focusManager = LocalFocusManager.current
  val isNotRoot = uiState.editUser.type.isRoot().not()

  MyOutlinedTextField(
    modifier = Modifier.focusRequester(usernameRef),
    value = uiState.editUser.username,
    onValueChange = { input ->
      onEvent(UserSettingsEditUserEvent.Update { it.copy(username = input.trim()) })
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
      onValueChange = { input ->
        onEvent(UserSettingsEditUserEvent.Update { it.copy(password = input.trim()) })
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
    onValueChange = { input ->
      onEvent(UserSettingsEditUserEvent.Update { it.copy(email = input.trim()) })
    },
    label = stringResource(R.string.email),
    keyboardOptions =
      KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
    onNext = { focusManager.moveFocus(FocusDirection.Next) },
  )

  Spacer(Modifier.height(12.dp))
}

@Composable
private fun RootSection() {
  HorizontalDivider(Modifier.padding(vertical = 16.dp))
  Button(onClick = {}) { Text(stringResource(R.string.change_root_password)) }
}

@Composable
private fun NonRootSection(uiState: EditUserUiState, onEvent: (UserSettingsEditUserEvent) -> Unit) {
  val options = remember { UserType.editTypes.map { it.name } }
  MySegmentedButton(
    modifier = Modifier.fillMaxWidth(),
    options,
    stringResource(R.string.account_type),
    uiState.editUser.type.name,
    { selection ->
      val userType = UserType.valueOf(selection)
      val permissions = UserType.permissions(userType)
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val editUser = it.editUser.copy(type = userType)
          it.copy(editUser = editUser, permissions = permissions)
        }
      )
    },
  )

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

  PermissionSection(uiState, onEvent)

  LibrariesSection(uiState, onEvent)

  TagsSection(uiState, onEvent)
}

@Composable
fun PermissionSection(uiState: EditUserUiState, onEvent: (UserSettingsEditUserEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.permissions))
  SettingsSwitchItem(
    title = stringResource(R.string.download),
    checked = uiState.permissions.download,
    contentDescription = stringResource(R.string.download),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(download = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.update),
    checked = uiState.permissions.update,
    contentDescription = stringResource(R.string.update),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(update = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.delete),
    checked = uiState.permissions.delete,
    contentDescription = stringResource(R.string.delete),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(delete = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.upload),
    checked = uiState.permissions.upload,
    contentDescription = stringResource(R.string.upload),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(upload = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.create_ereader),
    checked = uiState.permissions.createEReader,
    contentDescription = stringResource(R.string.create_ereader),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(createEReader = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
  SettingsSwitchItem(
    title = stringResource(R.string.access_explicit_content),
    checked = uiState.permissions.accessExplicit,
    contentDescription = stringResource(R.string.access_explicit_content),
    onCheckedChange = { checked ->
      onEvent(
        UserSettingsEditUserEvent.UpdateUiState {
          val permissions = it.permissions.copy(accessExplicit = checked)
          it.copy(permissions = permissions)
        }
      )
    },
  )
}

@Composable
private fun TagsSection(uiState: EditUserUiState, onEvent: (UserSettingsEditUserEvent) -> Unit) {
  SettingsSwitchItem(
    title = stringResource(R.string.access_all_tags),
    checked = uiState.permissions.accessAllTags,
    contentDescription = stringResource(R.string.access_all_tags),
    onCheckedChange = { selection ->
      if (selection) {
        onEvent(
          UserSettingsEditUserEvent.UpdateUiState {
            val permissions = it.permissions.copy(accessAllTags = selection)
            val editUser = it.editUser.copy(itemTagsAccessible = emptyList())
            it.copy(editUser = editUser, permissions = permissions)
          }
        )
      } else {
        onEvent(
          UserSettingsEditUserEvent.UpdateUiState {
            val permissions = it.permissions.copy(accessAllTags = selection)
            it.copy(permissions = permissions)
          }
        )
      }
    },
  )
  AnimatedVisibility(uiState.permissions.accessAllTags.not()) {
    Column {
      DropdownOutlinedTextField(
        selectedOptions = uiState.editUser.itemTagsAccessible.sorted(),
        label = stringResource(R.string.tags_accessible_to_user),
        options = uiState.tags,
        placeholder = stringResource(R.string.select_tags),
        onOptionToggled = { tag ->
          onEvent(
            UserSettingsEditUserEvent.Update { state ->
              state.copy(
                itemTagsAccessible =
                  if (tag in state.itemTagsAccessible) {
                    state.itemTagsAccessible - tag
                  } else {
                    state.itemTagsAccessible + tag
                  }
              )
            }
          )
        },
        onOptionRemoved = { tag ->
          onEvent(
            UserSettingsEditUserEvent.Update {
              it.copy(itemTagsAccessible = it.itemTagsAccessible - tag)
            }
          )
        },
      )

      SettingsSwitchItem(
        title = stringResource(R.string.invert),
        checked = uiState.editUser.invert,
        contentDescription = stringResource(R.string.invert),
        onCheckedChange = {
          onEvent(UserSettingsEditUserEvent.Update { it.copy(invert = it.invert.not()) })
        },
      )
    }
  }
}

@Composable
private fun LibrariesSection(
  uiState: EditUserUiState,
  onEvent: (UserSettingsEditUserEvent) -> Unit,
) {
  SettingsSwitchItem(
    title = stringResource(R.string.access_all_libraries),
    checked = uiState.permissions.accessAllLibraries,
    contentDescription = stringResource(R.string.access_all_libraries),
    onCheckedChange = { selection ->
      if (selection) {
        onEvent(
          UserSettingsEditUserEvent.UpdateUiState {
            val permissions = it.permissions.copy(accessAllLibraries = selection)
            val editUser = it.editUser.copy(librariesAccessible = emptyList())
            it.copy(editUser = editUser, permissions = permissions)
          }
        )
      } else {
        onEvent(
          UserSettingsEditUserEvent.UpdateUiState {
            val permissions = it.permissions.copy(accessAllLibraries = selection)
            it.copy(permissions = permissions)
          }
        )
      }
    },
  )
  val selected =
    remember(uiState.permissions.accessAllLibraries, uiState.editUser.librariesAccessible) {
      uiState.editUser.librariesAccessible.mapNotNull { id ->
        uiState.libraries.find { it.id == id }?.name
      }
    }
  AnimatedVisibility(uiState.permissions.accessAllLibraries.not()) {
    Column {
      DropdownOutlinedTextField(
        selectedOptions = selected.sorted(),
        onOptionToggled = { libraryName ->
          val library = uiState.libraries.find { it.name == libraryName }
          library?.let { library ->
            onEvent(
              UserSettingsEditUserEvent.Update { state ->
                state.copy(
                  librariesAccessible =
                    if (library.id in state.librariesAccessible) {
                      state.librariesAccessible - library.id
                    } else {
                      state.librariesAccessible + library.id
                    }
                )
              }
            )
          }
        },
        onOptionRemoved = { libraryName ->
          val library = uiState.libraries.find { it.name == libraryName }
          library?.let { library ->
            onEvent(
              UserSettingsEditUserEvent.Update {
                it.copy(librariesAccessible = it.librariesAccessible - library.id)
              }
            )
          }
        },
        label = stringResource(R.string.libraries_accessible_to_user),
        options = uiState.libraries.map { it.name },
        placeholder = stringResource(R.string.select_libraries),
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun EditUserScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { EditUserContent() }
}

@ShelfDroidPreview
@Composable
fun EditUserScreenRootUserContentPreview() {
  val editUser = NavEditUser(type = UserType.Root)

  AnimatedPreviewWrapper(dynamicColor = false) {
    EditUserContent(uiState = EditUserUiState().copy(editUser = editUser))
  }
}
