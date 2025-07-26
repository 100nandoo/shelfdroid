package dev.halim.shelfdroid.core.ui.screen.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginState
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel(), onLoginSuccess: () -> Unit) {
  val snackbarHostState = remember { SnackbarHostState() }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val focusManager = LocalFocusManager.current

  LaunchedEffect(uiState.loginState) {
    when (val state = uiState.loginState) {
      is LoginState.Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
        viewModel.onEvent(LoginEvent.ErrorShown)
      }

      is LoginState.Success -> {
        focusManager.clearFocus()
        onLoginSuccess()
      }
      else -> {}
    }
  }

  LoginScreenContent(uiState, focusManager, viewModel::onEvent, snackbarHostState)
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState = LoginUiState(),
  focusManager: FocusManager = LocalFocusManager.current,
  onEvent: (LoginEvent) -> Unit = {},
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
) {
  val (serverRef, usernameRef, passwordRef) = remember { FocusRequester.createRefs() }

  LaunchedEffect(Unit) { serverRef.requestFocus() }

  Box(modifier = Modifier.fillMaxSize().imePadding()) {
    AnimatedVisibility(uiState.loginState is LoginState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }

    SnackbarHost(
      hostState = snackbarHostState,
      modifier = Modifier.align(Alignment.TopCenter).imePadding(),
    )

    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      LoginTextField(
        value = uiState.server,
        onValueChange = { onEvent(LoginEvent.ServerChanged(it)) },
        label = stringResource(R.string.server_address),
        placeholder = stringResource(R.string.placeholder_server),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().focusRequester(serverRef).testTag("server"),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      LoginTextField(
        value = uiState.username,
        onValueChange = { onEvent(LoginEvent.UsernameChanged(it)) },
        label = stringResource(R.string.username),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        modifier = Modifier.testTag(stringResource(R.string.username)).focusRequester(usernameRef),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      PasswordTextField(
        value = uiState.password,
        onValueChange = { onEvent(LoginEvent.PasswordChanged(it)) },
        label = stringResource(R.string.password),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        modifier = Modifier.testTag(stringResource(R.string.password)).focusRequester(passwordRef),
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

@Composable
private fun PasswordTextField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  placeholder: String? = null,
  keyboardOptions: KeyboardOptions,
  onNext: (() -> Unit)? = null,
  onDone: (() -> Unit)? = null,
) {
  var passwordVisible by remember { mutableStateOf(false) }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    placeholder = placeholder?.let { { Text(it) } },
    keyboardOptions = keyboardOptions,
    visualTransformation =
      if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
      val description = if (passwordVisible) "Hide password" else "Show password"

      IconButton(onClick = { passwordVisible = !passwordVisible }) {
        Icon(imageVector = image, contentDescription = description)
      }
    },
    modifier = modifier.fillMaxWidth(),
    keyboardActions =
      KeyboardActions(onNext = onNext?.let { { it() } }, onDone = onDone?.let { { it() } }),
  )
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  val loginUiState = LoginUiState()
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(loginUiState) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val loginUiState = LoginUiState(loginState = LoginState.Failure("Wrong credentials"))
  PreviewWrapper(dynamicColor = true) { LoginScreenContent(loginUiState) }
}
