package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OpenIdSettingsSummary
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextTitleLarge
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun AuthenticationSettingsScreen(
  viewModel: AuthenticationSettingsViewModel = hiltViewModel(),
  onBackClicked: () -> Unit = {},
) {
  val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
  AuthenticationSettingsContent(
    state = uiState.state,
    onRetry = { viewModel.onEvent(AuthenticationSettingsEvent.Retry) },
    onBackClicked = onBackClicked,
  )
}

@Composable
fun AuthenticationSettingsContent(
  state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
  onRetry: () -> Unit = {},
  onBackClicked: () -> Unit = {},
) {
  when (state) {
    AuthenticationSettingsState.Loading -> LoadingContent()
    AuthenticationSettingsState.AccessDenied ->
      MessageContent(
        title = stringResource(R.string.authentication_access_denied),
        message = stringResource(R.string.authentication_access_denied_message),
        actionLabel = stringResource(R.string.back),
        onAction = onBackClicked,
      )
    is AuthenticationSettingsState.Failure ->
      MessageContent(
        title = stringResource(R.string.authentication_settings_failed),
        message = state.message ?: stringResource(R.string.authentication_settings_failed_message),
        actionLabel = stringResource(R.string.retry),
        onAction = onRetry,
      )
    is AuthenticationSettingsState.Ready ->
      ReadyContent(settings = state.settings)
  }
}

@Composable
private fun LoadingContent() {
  Column(modifier = Modifier.fillMaxSize()) {
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    Text(
      text = stringResource(R.string.authentication_settings_loading),
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@Composable
private fun MessageContent(
  title: String,
  message: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    verticalArrangement = Arrangement.Center,
  ) {
    TextTitleLarge(text = title)
    Spacer(modifier = Modifier.height(8.dp))
    Text(text = message, style = MaterialTheme.typography.bodyLarge)
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
  }
}

@Composable
private fun ReadyContent(settings: AuthenticationSettingsSummary) {
  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
  ) {
    TextTitleLarge(text = stringResource(R.string.authentication_settings))
    Spacer(modifier = Modifier.height(20.dp))

    TextTitleMedium(text = stringResource(R.string.authentication_login_message))
    SummaryRow(
      label = stringResource(R.string.authentication_custom_message),
      value =
        if (settings.customMessageEnabled) stringResource(R.string.enabled)
        else stringResource(R.string.disabled),
    )
    Spacer(modifier = Modifier.height(20.dp))

    TextTitleMedium(text = stringResource(R.string.authentication_login_methods))
    SummaryRow(
      label = stringResource(R.string.authentication_active_login_methods),
      value = settings.activeLoginMethods.toDisplayValue(),
    )
    Spacer(modifier = Modifier.height(20.dp))

    OpenIdSummary(settings.openId)
  }
}

@Composable
private fun OpenIdSummary(settings: OpenIdSettingsSummary) {
  TextTitleMedium(text = stringResource(R.string.authentication_openid_provider))
  SummaryRow(stringResource(R.string.authentication_issuer_url), settings.issuerUrl)
  SummaryRow(stringResource(R.string.authentication_authorization_url), settings.authorizationUrl)
  SummaryRow(stringResource(R.string.authentication_token_url), settings.tokenUrl)
  SummaryRow(stringResource(R.string.authentication_userinfo_url), settings.userInfoUrl)
  SummaryRow(stringResource(R.string.authentication_jwks_url), settings.jwksUrl)
  SummaryRow(stringResource(R.string.authentication_logout_url), settings.logoutUrl)
  SummaryRow(stringResource(R.string.authentication_client_id), settings.clientId)
  SummaryRow(
    stringResource(R.string.authentication_client_secret),
    if (settings.clientSecretConfigured) stringResource(R.string.configured)
    else stringResource(R.string.not_configured),
  )
  SummaryRow(
    stringResource(R.string.authentication_signing_algorithm),
    settings.tokenSigningAlgorithm,
  )

  Spacer(modifier = Modifier.height(16.dp))
  TextTitleMedium(text = stringResource(R.string.authentication_callbacks))
  SummaryRow(
    stringResource(R.string.authentication_callback_subfolder),
    settings.subfolderForRedirectUrls,
  )
  SummaryRow(
    stringResource(R.string.authentication_mobile_redirect_uris),
    settings.mobileRedirectUris.joinToString(separator = "\n"),
  )
  SummaryRow(stringResource(R.string.authentication_button_text), settings.buttonText)

  Spacer(modifier = Modifier.height(16.dp))
  TextTitleMedium(text = stringResource(R.string.authentication_user_mapping))
  SummaryRow(stringResource(R.string.authentication_match_existing_by), settings.matchExistingBy)
  SummaryRow(
    stringResource(R.string.authentication_auto_launch),
    settings.autoLaunch.toEnabledValue(),
  )
  SummaryRow(
    stringResource(R.string.authentication_auto_register),
    settings.autoRegister.toEnabledValue(),
  )
  SummaryRow(stringResource(R.string.authentication_group_claim), settings.groupClaim)
  SummaryRow(
    stringResource(R.string.authentication_advanced_permissions_claim),
    settings.advancedPermsClaim,
  )
  SummaryRow(
    stringResource(R.string.authentication_sample_permissions),
    settings.samplePermissions,
  )
}

@Composable
private fun SummaryRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
    Text(
      text = label,
      modifier = Modifier.weight(0.4f),
      style = MaterialTheme.typography.labelLarge,
    )
    Text(
      text = value.ifBlank { stringResource(R.string.not_configured) },
      modifier = Modifier.weight(0.6f),
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

private fun List<LoginMethod>.toDisplayValue(): String =
  joinToString(", ") {
    when (it) {
      LoginMethod.Local -> "Username and password"
      LoginMethod.OpenId -> "OpenID login"
    }
  }.ifBlank { "None" }

@Composable
private fun Boolean.toEnabledValue(): String =
  if (this) stringResource(R.string.enabled) else stringResource(R.string.disabled)

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsLoadingPreview() {
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent()
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsReadyPreview() {
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state =
        AuthenticationSettingsState.Ready(
          AuthenticationSettingsSummary(
            customMessageEnabled = true,
            activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
            openId =
              OpenIdSettingsSummary(
                issuerUrl = "https://issuer.example.com",
                clientId = "shelfdroid",
                clientSecretConfigured = true,
                mobileRedirectUris = listOf("audiobookshelf://oauth"),
                matchExistingBy = "email",
                autoLaunch = true,
              ),
          )
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsFailurePreview() {
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Failure("The server could not be reached."),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsAccessDeniedPreview() {
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(state = AuthenticationSettingsState.AccessDenied)
  }
}
