package dev.halim.shelfdroid.core.ui.screen.login

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dev.halim.shelfdroid.core.AuthPromptReason
import dev.halim.shelfdroid.core.ServerAccessMode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.login.LocalNetworkPermissionState
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryMessage
import dev.halim.shelfdroid.core.data.screen.login.LoginDiscoveryState
import dev.halim.shelfdroid.core.data.screen.login.LoginEvent
import dev.halim.shelfdroid.core.data.screen.login.LoginFieldError
import dev.halim.shelfdroid.core.data.screen.login.LoginUiState
import dev.halim.shelfdroid.core.data.screen.login.isOpenIdOnly
import dev.halim.shelfdroid.core.data.screen.login.showsMixedLoginMethods
import dev.halim.shelfdroid.core.data.screen.login.supportsLocalLogin
import dev.halim.shelfdroid.core.ui.extensions.findActivity
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.navigation.Login
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

internal data class ServerAccessOption(val accessMode: ServerAccessMode, val label: String)
internal data class ServerAccessControlState(
  val options: List<ServerAccessOption>,
  val enabled: Boolean,
)
private const val ANDROID_17_API_LEVEL = 37

@Composable
fun LoginScreen(
  navKey: Login = Login(),
  viewModel: LoginViewModel =
    hiltViewModel<LoginViewModel, LoginViewModel.Factory> { factory ->
      factory.create(navKey)
    },
  snackbarHostState: SnackbarHostState,
  onLoginSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val openIdLoginLauncher = remember(context) { AndroidOpenIdLoginLauncher(context) }
  val requestLocalNetworkPermission =
    rememberLocalNetworkPermissionRequester { granted, permanentlyDenied ->
      viewModel.onEvent(
        LoginEvent.LocalNetworkPermissionResult(
          granted = granted,
          permanentlyDenied = permanentlyDenied,
        )
      )
    }
  val focusManager = LocalFocusManager.current
  val openIdRedirectUri = "audiobookshelf://oauth"

  LaunchedEffect(uiState.loginState) {
    when (val state = uiState.loginState) {
      is GenericState.Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showErrorSnackbar(it) } }
        viewModel.onEvent(LoginEvent.ErrorShown)
      }

      is GenericState.Success -> {
        focusManager.clearFocus()
        onLoginSuccess()
      }
      else -> {}
    }
  }

  LaunchedEffect(viewModel, openIdLoginLauncher, requestLocalNetworkPermission) {
    viewModel.events.collect { event ->
      handleLoginUiEvent(
        event = event,
        launchOpenIdLogin = openIdLoginLauncher::launch,
        requestLocalNetworkPermission = requestLocalNetworkPermission,
      )
    }
  }

  LoginScreenContent(
    uiState = uiState,
    focusManager = focusManager,
    openIdRedirectUri = openIdRedirectUri,
    onOpenAppSettings = { context.openAppSettings() },
    onEvent = viewModel::onEvent,
  )
}

@Composable
fun LoginScreenContent(
  uiState: LoginUiState = LoginUiState(),
  focusManager: FocusManager = LocalFocusManager.current,
  openIdRedirectUri: String = "audiobookshelf://oauth",
  onOpenAppSettings: () -> Unit = {},
  onEvent: (LoginEvent) -> Unit = {},
) {
  val (serverRef, usernameRef, passwordRef) = remember { FocusRequester.createRefs() }
  var showUseDifferentAccountDialog by remember { mutableStateOf(false) }
  val supportsLocalLogin = uiState.supportsLocalLogin()
  val headerMessages =
    loginHeaderMessages(
      uiState = uiState,
      refreshFailedMessage = stringResource(R.string.failed_to_refresh_token_relogin_required),
      manualReLoginMessage = stringResource(R.string.reenter_password_to_continue),
    )
  val localNetworkPermissionGuidance =
    localNetworkPermissionGuidance(
      uiState = uiState,
      deniedMessage = stringResource(R.string.local_network_permission_denied),
      permanentlyDeniedMessage = stringResource(R.string.local_network_permission_permanently_denied),
    )
  val serverAccessControlState =
    serverAccessControlState(
      uiState = uiState,
      internetLabel = stringResource(R.string.server_access_internet),
      localNetworkLabel = stringResource(R.string.server_access_local_network),
    )

  LaunchedEffect(uiState.reLogin, supportsLocalLogin) {
    when {
      uiState.reLogin && supportsLocalLogin -> passwordRef.requestFocus()
      uiState.reLogin.not() -> serverRef.requestFocus()
    }
  }

  Box(modifier = Modifier.fillMaxSize().imePadding()) {
    VisibilityDown(
      uiState.loginState is GenericState.Loading ||
        uiState.discoveryState is LoginDiscoveryState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
    }

    Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Bottom,
    ) {
      LoginHeaderMessagesSection(
        headerMessages = headerMessages,
        onUseDifferentServerOrAccountClick = { showUseDifferentAccountDialog = true },
      )

      MyOutlinedTextField(
        modifier = Modifier.fillMaxWidth().focusRequester(serverRef).testTag("server"),
        enabled = uiState.reLogin.not(),
        value = uiState.server,
        onValueChange = {
          onEvent(LoginEvent.ServerChanged(it.withoutServerTextFieldSpacing()))
          if (it.containsLoginTextFieldNewline()) focusManager.moveFocus(FocusDirection.Next)
        },
        label = stringResource(R.string.server_address),
        placeholder = stringResource(R.string.placeholder_server),
        supportingText =
          if (uiState.serverFieldError == LoginFieldError.InvalidServerUrl) {
            stringResource(R.string.invalid_server_url)
          } else {
            null
          },
        isError = uiState.serverFieldError != null,
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        onNext = { focusManager.moveFocus(FocusDirection.Next) },
      )

      Spacer(modifier = Modifier.height(8.dp))

      MySegmentedButton(
        modifier = Modifier.fillMaxWidth(),
        label = stringResource(R.string.server_access),
        options = serverAccessControlState.options,
        selectedValue =
          serverAccessControlState.options.first { it.accessMode == uiState.serverAccessMode },
        enabled = serverAccessControlState.enabled,
        onClick = { onEvent(LoginEvent.ServerAccessModeChanged(it.accessMode)) },
        optionLabel = { it.label },
      )

      localNetworkPermissionGuidance.message?.let { message ->
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = message,
          modifier = Modifier.fillMaxWidth(),
          textAlign = TextAlign.Center,
        )

        if (localNetworkPermissionGuidance.showSettingsButton) {
          Spacer(modifier = Modifier.height(8.dp))
          OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.go_to_settings))
          }
        }
      }

      if (supportsLocalLogin) {
        MyOutlinedTextField(
          modifier =
            Modifier.testTag(stringResource(R.string.username)).focusRequester(usernameRef),
          enabled = uiState.reLogin.not(),
          value = uiState.username,
          onValueChange = {
            onEvent(LoginEvent.UsernameChanged(it.withoutLoginTextFieldNewlines()))
            if (it.containsLoginTextFieldNewline()) focusManager.moveFocus(FocusDirection.Next)
          },
          label = stringResource(R.string.username),
          keyboardOptions =
            KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          onNext = { focusManager.moveFocus(FocusDirection.Next) },
        )

        Spacer(modifier = Modifier.height(8.dp))

        PasswordTextField(
          modifier =
            Modifier.testTag(stringResource(R.string.password)).focusRequester(passwordRef),
          value = uiState.password,
          onValueChange = {
            onEvent(LoginEvent.PasswordChanged(it.withoutLoginTextFieldNewlines()))
            if (it.containsLoginTextFieldNewline()) onEvent(LoginEvent.LoginButtonPressed)
          },
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

        AnimatedVisibility(visible = uiState.showsMixedLoginMethods()) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(modifier = Modifier.height(20.dp))
            AlternativeSignInDivider()
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
              onClick = { onEvent(LoginEvent.OpenIdLoginButtonPressed(openIdRedirectUri)) },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(uiState.authOpenIdButtonText ?: stringResource(R.string.login_with_openid))
            }
          }
        }
      } else if (uiState.isOpenIdOnly()) {
        Button(
          onClick = { onEvent(LoginEvent.OpenIdLoginButtonPressed(openIdRedirectUri)) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(uiState.authOpenIdButtonText ?: stringResource(R.string.login_with_openid))
        }
      }
    }
  }

  MyAlertDialog(
    showDialog = showUseDifferentAccountDialog,
    title = stringResource(R.string.use_different_server_or_account),
    text = stringResource(R.string.dialog_use_different_server_or_account_text),
    confirmText = stringResource(R.string.logout),
    onConfirm = {
      onEvent(LoginEvent.UseDifferentServerOrAccountConfirmed)
      showUseDifferentAccountDialog = false
    },
    onDismiss = { showUseDifferentAccountDialog = false },
  )
}

internal fun String.containsLoginTextFieldNewline(): Boolean = any { it.isLoginTextFieldNewline() }

internal fun String.withoutLoginTextFieldNewlines(): String = filterNot {
  it.isLoginTextFieldNewline()
}

internal fun String.withoutServerTextFieldSpacing(): String = filterNot {
  it == ' ' || it == '\t' || it.isLoginTextFieldNewline()
}

private fun Char.isLoginTextFieldNewline(): Boolean = this == '\n' || this == '\r'

@Composable
private fun LoginHeaderMessagesSection(
  headerMessages: LoginHeaderMessages,
  onUseDifferentServerOrAccountClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val textLinkColor = MaterialTheme.colorScheme.primary
  val linkStyles =
    remember(textLinkColor) {
      TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = textLinkColor))
    }

  Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
    headerMessages.promptReasonMessage?.let { message ->
      Text(
        text = message,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )

      TextButton(onClick = onUseDifferentServerOrAccountClick) {
        Text(stringResource(R.string.use_different_server_or_account))
      }

      Spacer(modifier = Modifier.height(16.dp))
    }

    headerMessages.customMessage?.let { message ->
      Text(
        text =
          remember(message, linkStyles) {
            AnnotatedString.fromHtml(message, linkStyles = linkStyles)
          },
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(12.dp))
    }

    headerMessages.discoveryMessage?.let { message ->
      Text(
        text = stringResource(message.asStringRes()),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
      )
      Spacer(modifier = Modifier.height(12.dp))
    }
  }
}

@Composable
private fun AlternativeSignInDivider() {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    HorizontalDivider(modifier = Modifier.weight(1f))
    Text(
      text = stringResource(R.string.or),
      modifier = Modifier.padding(horizontal = 16.dp),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider(modifier = Modifier.weight(1f))
  }
}

internal data class LoginHeaderMessages(
  val promptReasonMessage: String? = null,
  val customMessage: String? = null,
  val discoveryMessage: LoginDiscoveryMessage? = null,
) {
  fun ordered(discoveryMessageText: (LoginDiscoveryMessage) -> String): List<String> {
    return buildList {
      promptReasonMessage?.let(::add)
      customMessage?.let(::add)
      discoveryMessage?.let { add(discoveryMessageText(it)) }
    }
  }
}

internal fun loginHeaderMessages(
  uiState: LoginUiState,
  refreshFailedMessage: String,
  manualReLoginMessage: String,
): LoginHeaderMessages {
  val promptReasonMessage =
    when (uiState.authPromptReason) {
      AuthPromptReason.RefreshFailed -> refreshFailedMessage
      AuthPromptReason.ManualReLogin -> manualReLoginMessage
      null -> null
    }
  return LoginHeaderMessages(
    promptReasonMessage = promptReasonMessage,
    customMessage = uiState.authLoginCustomMessage,
    discoveryMessage = uiState.loginDiscoveryMessage,
  )
}

internal fun serverAccessControlState(
  uiState: LoginUiState,
  internetLabel: String,
  localNetworkLabel: String,
): ServerAccessControlState {
  return ServerAccessControlState(
    options =
      listOf(
        ServerAccessOption(
          accessMode = ServerAccessMode.Internet,
          label = internetLabel,
        ),
        ServerAccessOption(
          accessMode = ServerAccessMode.LocalNetwork,
          label = localNetworkLabel,
        ),
      ),
    enabled = uiState.reLogin.not(),
  )
}

internal data class LocalNetworkPermissionGuidance(
  val message: String? = null,
  val showSettingsButton: Boolean = false,
)

internal fun localNetworkPermissionGuidance(
  uiState: LoginUiState,
  deniedMessage: String,
  permanentlyDeniedMessage: String,
): LocalNetworkPermissionGuidance {
  return when (uiState.localNetworkPermissionState) {
    LocalNetworkPermissionState.Denied ->
      LocalNetworkPermissionGuidance(message = deniedMessage)
    LocalNetworkPermissionState.PermanentlyDenied ->
      LocalNetworkPermissionGuidance(
        message = permanentlyDeniedMessage,
        showSettingsButton = true,
      )
    null -> LocalNetworkPermissionGuidance()
  }
}

private fun LoginDiscoveryMessage.asStringRes(): Int {
  return when (this) {
    LoginDiscoveryMessage.MethodsUnconfirmed -> R.string.login_discovery_methods_unconfirmed
    LoginDiscoveryMessage.MethodsUnconfirmedTryLocalNetwork ->
      R.string.login_discovery_methods_unconfirmed_try_local_network
    LoginDiscoveryMessage.LocalLoginUnavailable -> R.string.login_discovery_local_unavailable
  }
}

@Composable
private fun rememberLocalNetworkPermissionRequester(
  onPermissionResult: (granted: Boolean, permanentlyDenied: Boolean) -> Unit
): () -> Unit {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val launcher =
    rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
      val permanentlyDenied =
        !granted &&
          !ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.ACCESS_LOCAL_NETWORK,
          )
      onPermissionResult(granted, permanentlyDenied)
    }

  return {
    val granted =
      Build.VERSION.SDK_INT < ANDROID_17_API_LEVEL ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_LOCAL_NETWORK) ==
          PackageManager.PERMISSION_GRANTED
    if (granted) {
      onPermissionResult(true, false)
    } else {
      launcher.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
    }
  }
}

private fun Context.openAppSettings() {
  startActivity(
    Intent(
      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
      Uri.fromParts("package", packageName, null),
    )
  )
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
  val loginUiState =
    LoginUiState(
      authLoginCustomMessage = "Use your library username and password.",
      loginState = GenericState.Failure("Wrong credentials"),
    )
  PreviewWrapper(dynamicColor = true) { LoginScreenContent(loginUiState) }
}

@ShelfDroidPreview
@Composable
fun ReLoginScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(Defaults.LOGIN_RELOGIN_UI_STATE) }
}

@ShelfDroidPreview
@Composable
fun MixedLoginMethodsScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(Defaults.LOGIN_MIXED_METHODS_UI_STATE) }
}

@ShelfDroidPreview
@Composable
fun OpenIdOnlyLoginScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { LoginScreenContent(Defaults.LOGIN_OPEN_ID_ONLY_UI_STATE) }
}
