package dev.halim.shelfdroid.core.ui.screen.usersettings.changepassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.usersettings.changepassword.ChangePasswordUiEvent
import dev.halim.shelfdroid.core.data.screen.usersettings.changepassword.ChangePasswordUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun ChangePasswordScreen(
  viewModel: ChangePasswordViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  finish: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ChangePasswordContent(uiState, onEvent = viewModel::onEvent)
  SnackbarHandling(viewModel, snackbarHostState, finish)
}

@Composable
private fun SnackbarHandling(
  viewModel: ChangePasswordViewModel,
  snackbarHostState: SnackbarHostState,
  finish: () -> Unit,
) {
  val scope = rememberCoroutineScope()
  val notMatchMessage = stringResource(R.string.passwords_do_not_match)
  val successMessage = stringResource(R.string.passwords_changed_successfully)
  val invalidPassword = stringResource(R.string.invalid_password)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is ChangePasswordUiEvent.ApiError -> {
          if (event.isInvalid) scope.launch { snackbarHostState.showErrorSnackbar(invalidPassword) }
          else event.message?.let { scope.launch { snackbarHostState.showErrorSnackbar(it) } }
        }
        ChangePasswordUiEvent.NotMatchError ->
          scope.launch { snackbarHostState.showErrorSnackbar(notMatchMessage) }
        ChangePasswordUiEvent.Success -> {
          finish()
          scope.launch { snackbarHostState.showSuccessSnackbar(successMessage) }
        }
      }
    }
  }
}

@Composable
private fun ChangePasswordContent(
  uiState: ChangePasswordUiState = ChangePasswordUiState(),
  onEvent: (ChangePasswordEvent) -> Unit = {},
) {

  val focusManager = LocalFocusManager.current
  val (oldRef, newRef, confirmRef) = remember { FocusRequester.createRefs() }

  Box(modifier = Modifier.fillMaxSize().imePadding()) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      verticalArrangement = Arrangement.Bottom,
    ) {
      PasswordTextField(
        modifier = Modifier.focusRequester(oldRef),
        value = uiState.old,
        onValueChange = { old -> onEvent(ChangePasswordEvent.Update { it.copy(old = old) }) },
        label = stringResource(R.string.old_password),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        onDone = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))
      PasswordTextField(
        modifier = Modifier.focusRequester(newRef),
        value = uiState.new,
        onValueChange = { new -> onEvent(ChangePasswordEvent.Update { it.copy(new = new) }) },
        label = stringResource(R.string.new_password),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        onDone = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))
      PasswordTextField(
        modifier = Modifier.focusRequester(confirmRef),
        value = uiState.confirm,
        onValueChange = { confirm ->
          onEvent(ChangePasswordEvent.Update { it.copy(confirm = confirm) })
        },
        label = stringResource(R.string.confirm_password),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        onDone = { onEvent(ChangePasswordEvent.Submit) },
      )
      Spacer(modifier = Modifier.height(8.dp))

      val enabledSubmit =
        uiState.old.isNotEmpty() && uiState.new.isNotEmpty() && uiState.confirm.isNotEmpty()
      Button(
        enabled = enabledSubmit,
        onClick = {
          focusManager.clearFocus()
          onEvent(ChangePasswordEvent.Submit)
        },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.submit))
      }

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@ShelfDroidPreview
@Composable
fun ChangePasswordScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { ChangePasswordContent() }
}
