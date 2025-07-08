package dev.halim.shelfdroid.core.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  paddingValues: PaddingValues,
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
  viewModel: LoginViewModel = hiltViewModel(),
  onLoginSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  LaunchedEffect(uiState.loginState) {
    when (val state = uiState.loginState) {
      is LoginState.Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
        viewModel.updateUiState(uiState.copy(loginState = LoginState.NotLoggedIn))
      }

      is LoginState.Success -> onLoginSuccess()
      else -> {}
    }
  }

  LoginScreenContent(uiState, paddingValues, viewModel::onEvent, viewModel::updateUiState)
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState = LoginUiState(),
  paddingValues: PaddingValues = PaddingValues(),
  onEvent: (LoginEvent) -> Unit = {},
  updateUiState: (LoginUiState) -> Unit = {},
) {
  val focusManager = LocalFocusManager.current
  val (server, username, password) = remember { FocusRequester.createRefs() }

  LaunchedEffect(Unit) { server.requestFocus() }

  Box(
    modifier = Modifier.fillMaxSize().padding(paddingValues).padding(bottom = 48.dp).imePadding()
  ) {
    Column(
      modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      if (uiState.loginState is LoginState.Loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

      LoginTextField(
        value = uiState.server,
        onValueChange = { updateUiState(uiState.copy(server = it)) },
        label = "Server Address",
        placeholder = "audio.bookshelf.org",
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.focusRequester(server).testTag("server"),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      LoginTextField(
        value = uiState.username,
        onValueChange = { updateUiState(uiState.copy(username = it)) },
        label = "Username",
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.testTag("username").focusRequester(username),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      LoginTextField(
        value = uiState.password,
        onValueChange = { updateUiState(uiState.copy(password = it)) },
        label = "Password",
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.testTag("password").focusRequester(password),
        onDone = {
          focusManager.clearFocus()
          onEvent(LoginEvent.LoginButtonPressed)
        },
      )

      Spacer(modifier = Modifier.height(16.dp))

      Button(
        onClick = {
          focusManager.clearFocus()
          onEvent(LoginEvent.LoginButtonPressed)
        },
        modifier = Modifier.fillMaxWidth().testTag("login"),
      ) {
        Text("Login")
      }
    }
  }
}

@Composable
private fun LoginTextField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String? = null,
  keyboardOptions: KeyboardOptions,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  onNext: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation = visualTransformation,
    modifier = modifier.fillMaxWidth(),
    keyboardActions =
      KeyboardActions(onNext = onNext?.let { { it() } }, onDone = onDone?.let { { it() } }),
  )
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  val loginUiState =
    LoginUiState(server = "audiobookshelf.org", username = "admin", password = "123456")
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(loginUiState) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val loginUiState = LoginUiState(loginState = LoginState.Failure("Wrong credentials"))
  PreviewWrapper(dynamicColor = true) { LoginScreenContent(loginUiState) }
}
