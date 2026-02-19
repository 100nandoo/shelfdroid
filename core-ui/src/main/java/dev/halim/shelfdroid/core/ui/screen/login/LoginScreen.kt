package dev.halim.shelfdroid.core.ui.screen.login

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component1
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component2
import androidx.compose.ui.focus.FocusRequester.Companion.FocusRequesterFactory.component3
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  viewModel: LoginViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  onLoginSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  LaunchedEffect(uiState.loginState) {
    when (val state = uiState.loginState) {
      is GenericState.Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
        viewModel.onEvent(LoginEvent.ErrorShown)
      }

      is GenericState.Success -> {
        focusManager.clearFocus()
        onLoginSuccess()
      }
      else -> {}
    }
  }

  LoginScreenContent(uiState, focusManager, viewModel::onEvent)
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState = LoginUiState(),
  focusManager: FocusManager = LocalFocusManager.current,
  onEvent: (LoginEvent) -> Unit = {},
) {
  val (serverRef, usernameRef, passwordRef) = remember { FocusRequester.createRefs() }

  LaunchedEffect(Unit) {
    if (uiState.reLogin) passwordRef.requestFocus() else serverRef.requestFocus()
  }

  Box(modifier = Modifier.fillMaxSize().imePadding()) {
    VisibilityDown(uiState.loginState is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth().focusRequester(serverRef).testTag("server"),
        enabled = uiState.reLogin.not(),
        value = uiState.server,
        onValueChange = { onEvent(LoginEvent.ServerChanged(it)) },
        label = stringResource(R.string.server_address),
        prefix = stringResource(R.string.https),
        placeholder = stringResource(R.string.placeholder_server),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      MyOutlinedTextField(
        modifier = Modifier.testTag(stringResource(R.string.username)).focusRequester(usernameRef),
        enabled = uiState.reLogin.not(),
        value = uiState.username,
        onValueChange = { onEvent(LoginEvent.UsernameChanged(it)) },
        label = stringResource(R.string.username),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      PasswordTextField(
        modifier = Modifier.testTag(stringResource(R.string.password)).focusRequester(passwordRef),
        value = uiState.password,
        onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
        label = stringResource(R.string.password),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        onDone = { onEvent(LoginEvent.LoginButtonPressed) },
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = { onEvent(LoginEvent.LoginButtonPressed) },
        modifier = Modifier.fillMaxWidth().testTag(stringResource(R.string.login)),
      ) {
        Text(stringResource(R.string.login))
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun LoginScreenContentPreview() {
  val loginUiState = LoginUiState()
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(loginUiState) }
}

@ShelfDroidPreview
@Composable
fun LoginScreenContentDynamicPreview() {
  val loginUiState = LoginUiState(loginState = GenericState.Failure("Wrong credentials"))
  PreviewWrapper(dynamicColor = true) { LoginScreenContent(loginUiState) }
}

@ShelfDroidPreview
@Composable
fun ReLoginScreenContentPreview() {
  val loginUiState = LoginUiState(reLogin = true)
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(loginUiState) }
}
