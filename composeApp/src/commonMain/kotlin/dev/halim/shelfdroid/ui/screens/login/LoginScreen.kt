package dev.halim.shelfdroid.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.LoadingScreen
import dev.halim.shelfdroid.ui.screens.login.LoginState.Failure
import dev.halim.shelfdroid.ui.screens.login.LoginState.Loading
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val (server, username, password) = remember { FocusRequester.createRefs() }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) {
        when (uiState.loginState) {
            Loading -> LoadingScreen()
            else -> {
                Column(
                    Modifier
                        .fillMaxWidth().fillMaxHeight().imePadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    OutlinedTextField(
                        value = uiState.server,
                        onValueChange = { viewModel.updateUiState(uiState.copy(server = it)) },
                        placeholder = { Text("https://audio.bookshelf.com", color = Color.Gray) },
                        label = { Text("Server Address") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.focusRequester(server),
                        keyboardActions = KeyboardActions(onNext = { username.requestFocus() })
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.username,
                        onValueChange = { viewModel.updateUiState(uiState.copy(username = it)) },
                        label = { Text("Username") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.focusRequester(username),
                        keyboardActions = KeyboardActions(onNext = { password.requestFocus() })
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.password,
                        onValueChange = { viewModel.updateUiState(uiState.copy(password = it)) },
                        label = { Text("Password") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.focusRequester(password),
                        keyboardActions = KeyboardActions(onNext = { focusManager.clearFocus() })
                    )
                    Button(onClick = { viewModel.onEvent(LoginEvent.LoginButtonPressed) }) {
                        Text("Login")
                    }
                }
                if (uiState.loginState is Failure) {
                    val errorMessage = (uiState.loginState as Failure).errorMessage
                    scope.launch { snackbarHostState.showSnackbar(errorMessage) }
                }
                if (uiState.loginState is LoginState.Success) {
                    onLoginSuccess()
                }
            }
        }
    }
}