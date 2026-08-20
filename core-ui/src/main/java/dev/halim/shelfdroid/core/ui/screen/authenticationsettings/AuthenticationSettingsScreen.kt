@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.authenticationsettings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsApiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsConfirmation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsOperation
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsUiState
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.AuthenticationSettingsValidationError
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OPENID_MATCH_EXISTING_BY_OPTIONS
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.OpenIdSettingsSummary
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.callbackUrls
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.canSave
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.hasChanges
import dev.halim.shelfdroid.core.data.screen.authenticationsettings.validation
import dev.halim.shelfdroid.core.data.screen.login.LoginMethod
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.TextTitleLarge
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageActionScreen

@Composable
fun AuthenticationSettingsScreen(
  viewModel: AuthenticationSettingsViewModel = hiltViewModel(),
  onBackClicked: () -> Unit = {},
) {
  val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

  BackHandler { viewModel.onEvent(AuthenticationSettingsEvent.RequestBack) }

  LaunchedEffect(uiState.leaveRequested) {
    if (uiState.leaveRequested) {
      viewModel.onEvent(AuthenticationSettingsEvent.ConsumeLeaveRequest)
      onBackClicked()
    }
  }

  MyAlertDialog(
    showDialog = uiState.pendingConfirmation != null,
    title = stringResource(R.string.authentication_confirm_change),
    text = confirmationText(uiState.pendingConfirmation),
    confirmText = stringResource(R.string.continue_text),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      when (uiState.pendingConfirmation) {
        AuthenticationSettingsConfirmation.DisablePasswordSignIn ->
          viewModel.onEvent(AuthenticationSettingsEvent.ConfirmDisablePasswordSignIn)

        AuthenticationSettingsConfirmation.RemoveShelfDroidCallback ->
          viewModel.onEvent(AuthenticationSettingsEvent.ConfirmRemoveShelfDroidCallback)

        AuthenticationSettingsConfirmation.UseWildcardMobileRedirect ->
          viewModel.onEvent(AuthenticationSettingsEvent.ConfirmWildcardMobileRedirect)

        AuthenticationSettingsConfirmation.LeaveWithUnsavedChanges ->
          viewModel.onEvent(AuthenticationSettingsEvent.ConfirmLeave)

        null -> Unit
      }
    },
    onDismiss = { viewModel.onEvent(AuthenticationSettingsEvent.DismissConfirmation) },
  )

  AuthenticationSettingsContent(
    state = uiState.state,
    uiState = uiState,
    onRetry = { viewModel.onEvent(AuthenticationSettingsEvent.Retry) },
    onEvent = viewModel::onEvent,
  )
}

@Composable
fun AuthenticationSettingsContent(
  state: AuthenticationSettingsState = AuthenticationSettingsState.Loading,
  uiState: AuthenticationSettingsUiState = AuthenticationSettingsUiState(),
  onRetry: () -> Unit = {},
  onEvent: (AuthenticationSettingsEvent) -> Unit = {},
) {
  when (state) {
    AuthenticationSettingsState.Loading ->
      Column(modifier = Modifier.fillMaxSize()) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }

    is AuthenticationSettingsState.Failure ->
      GenericMessageActionScreen(
        message = state.message ?: stringResource(R.string.server_could_not_be_reached),
        actionLabel = stringResource(R.string.retry),
        onAction = onRetry,
      )

    is AuthenticationSettingsState.Ready ->
      EditorContent(
        uiState = uiState,
        onEvent = onEvent,
      )
  }
}

@Composable
private fun EditorContent(
  uiState: AuthenticationSettingsUiState,
  onEvent: (AuthenticationSettingsEvent) -> Unit,
) {
  val draft = uiState.draftSettings ?: return
  val enabled = uiState.apiState !is AuthenticationSettingsApiState.Loading
  val saveOutcomeVisible =
    (uiState.apiState as? AuthenticationSettingsApiState.Success)?.operation ==
      AuthenticationSettingsOperation.Save ||
      uiState.apiState is AuthenticationSettingsApiState.Rejected
  val failureState = uiState.apiState as? AuthenticationSettingsApiState.Failure
  Column(
    modifier =
      Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(16.dp)
  ) {
    TextTitleLarge(text = stringResource(R.string.authentication_settings))
    Spacer(modifier = Modifier.height(20.dp))

    TextTitleMedium(text = stringResource(R.string.authentication_login_message))
    MySwitch(
      title = stringResource(R.string.authentication_custom_message),
      checked = draft.customMessageEnabled,
      contentDescription = stringResource(R.string.authentication_custom_message),
      enabled = enabled,
      onCheckedChange = {
        onEvent(AuthenticationSettingsEvent.SetCustomMessageEnabled(it))
      },
    )
    AnimatedVisibility(visible = draft.customMessageEnabled) {
      Column {
        OutlinedTextField(
          value = draft.customMessage,
          onValueChange = { onEvent(AuthenticationSettingsEvent.UpdateCustomMessage(it)) },
          modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
          enabled = enabled,
          label = { Text(stringResource(R.string.authentication_custom_message_html)) },
          keyboardOptions = KeyboardOptions.Default,
          minLines = 4,
          maxLines = 10,
        )
        TextTitleMedium(
          modifier = Modifier.padding(top = 16.dp),
          text = stringResource(R.string.authentication_message_preview),
        )
        HtmlMessagePreview(draft.customMessage)
      }
    }

    Spacer(modifier = Modifier.height(24.dp))
    TextTitleMedium(text = stringResource(R.string.authentication_login_methods))
    MySwitch(
      title = stringResource(R.string.authentication_password_sign_in),
      checked = LoginMethod.Local in draft.activeLoginMethods,
      contentDescription = stringResource(R.string.authentication_password_sign_in),
      enabled = enabled,
      onCheckedChange = {
        onEvent(AuthenticationSettingsEvent.SetPasswordSignInEnabled(it))
      },
    )
    MySwitch(
      title = stringResource(R.string.authentication_openid_login),
      checked = LoginMethod.OpenId in draft.activeLoginMethods,
      contentDescription = stringResource(R.string.authentication_openid_login),
      enabled = enabled,
      onCheckedChange = { onEvent(AuthenticationSettingsEvent.SetOpenIdLoginEnabled(it)) },
    )

    uiState.validation.errors.forEach { error ->
      Text(
        text = error.toDisplayText(),
        modifier =
          Modifier.padding(top = 8.dp).semantics {
            liveRegion = LiveRegionMode.Polite
          },
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
      )
    }

    Spacer(modifier = Modifier.height(24.dp))
    OpenIdProviderEditor(
      settings = draft.openId,
      signingAlgorithmOptions = uiState.signingAlgorithmOptions,
      callbackSubfolderOptions = uiState.callbackSubfolderOptions,
      serverBaseUrl = uiState.serverBaseUrl,
      enabled = enabled,
      discoveryState = uiState.apiState,
      validationErrors = uiState.validation.errors,
      onEvent = onEvent,
    )

    Spacer(modifier = Modifier.height(24.dp))
    AnimatedVisibility(visible = saveOutcomeVisible) {
      Text(
        text =
          when (uiState.apiState) {
            is AuthenticationSettingsApiState.Success ->
              stringResource(R.string.authentication_settings_saved)

            AuthenticationSettingsApiState.Rejected ->
              stringResource(R.string.authentication_settings_rejected)

            else -> ""
          },
        color =
          if (
            (uiState.apiState as? AuthenticationSettingsApiState.Success)?.operation ==
              AuthenticationSettingsOperation.Save
          ) {
            MaterialTheme.colorScheme.primary
          } else MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
    }
    AnimatedVisibility(visible = failureState != null) {
      Text(
        text =
          failureState?.message ?: stringResource(R.string.authentication_settings_save_failed),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
      )
    }
    AnimatedVisibility(visible = uiState.restartRequired) {
      Text(
        text = stringResource(R.string.authentication_restart_required),
        color = MaterialTheme.colorScheme.error,
        modifier =
          Modifier.padding(top = 8.dp).semantics {
            liveRegion = LiveRegionMode.Polite
          },
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      TextButton(
        enabled = enabled && uiState.hasChanges,
        onClick = { onEvent(AuthenticationSettingsEvent.ResetDraftSettings) },
      ) {
        Text(stringResource(R.string.reset))
      }
      Button(
        enabled = enabled && uiState.canSave,
        onClick = { onEvent(AuthenticationSettingsEvent.SaveSettings) },
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }
}

@Composable
private fun OpenIdProviderEditor(
  settings: OpenIdSettingsSummary,
  signingAlgorithmOptions: List<String>,
  callbackSubfolderOptions: List<String>,
  serverBaseUrl: String,
  enabled: Boolean,
  discoveryState: AuthenticationSettingsApiState,
  validationErrors: Set<AuthenticationSettingsValidationError>,
  onEvent: (AuthenticationSettingsEvent) -> Unit,
) {
  val hasSigningAlgorithmOptions = signingAlgorithmOptions.isNotEmpty()
  val discoveryFailureState =
    (discoveryState as? AuthenticationSettingsApiState.Failure)?.takeIf {
      it.operation == AuthenticationSettingsOperation.Discovery
    }
  val invalidExistingUserMatching =
    AuthenticationSettingsValidationError.InvalidExistingUserMatching in validationErrors
  TextTitleMedium(text = stringResource(R.string.authentication_openid_provider))
  val update: ((OpenIdSettingsSummary) -> OpenIdSettingsSummary) -> Unit = { transform ->
    onEvent(
      AuthenticationSettingsEvent.UpdateDraftSettings { summary ->
        summary.copy(openId = transform(summary.openId))
      }
    )
  }
  MyOutlinedTextField(
    value = settings.issuerUrl,
    onValueChange = { changed -> update { openId -> openId.copy(issuerUrl = changed) } },
    label = stringResource(R.string.authentication_issuer_url),
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
  )
  Button(
    enabled = enabled && settings.issuerUrl.isNotBlank(),
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    onClick = { onEvent(AuthenticationSettingsEvent.DiscoverOpenId) },
  ) {
    Text(
      if (
        discoveryState is AuthenticationSettingsApiState.Loading &&
          discoveryState.operation == AuthenticationSettingsOperation.Discovery
      )
        stringResource(R.string.authentication_openid_discovering)
      else stringResource(R.string.authentication_openid_discover)
    )
  }
  OpenIdEndpointField(
    label = stringResource(R.string.authentication_authorization_url),
    value = settings.authorizationUrl,
    enabled = enabled,
    onValueChange = { changed -> update { openId -> openId.copy(authorizationUrl = changed) } },
  )
  OpenIdEndpointField(
    label = stringResource(R.string.authentication_token_url),
    value = settings.tokenUrl,
    enabled = enabled,
    onValueChange = { changed -> update { openId -> openId.copy(tokenUrl = changed) } },
  )
  OpenIdEndpointField(
    label = stringResource(R.string.authentication_userinfo_url),
    value = settings.userInfoUrl,
    enabled = enabled,
    onValueChange = { changed -> update { openId -> openId.copy(userInfoUrl = changed) } },
  )
  OpenIdEndpointField(
    label = stringResource(R.string.authentication_jwks_url),
    value = settings.jwksUrl,
    enabled = enabled,
    onValueChange = { changed -> update { openId -> openId.copy(jwksUrl = changed) } },
  )
  OpenIdEndpointField(
    label = stringResource(R.string.authentication_logout_url),
    value = settings.logoutUrl,
    enabled = enabled,
    onValueChange = { changed -> update { openId -> openId.copy(logoutUrl = changed) } },
  )
  MyOutlinedTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = settings.clientId,
    onValueChange = { changed -> update { openId -> openId.copy(clientId = changed) } },
    label = stringResource(R.string.authentication_client_id),
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
  )
  PasswordTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = settings.clientSecret,
    onValueChange = { onEvent(AuthenticationSettingsEvent.UpdateClientSecret(it)) },
    label = stringResource(R.string.authentication_client_secret),
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
  )
  AnimatedVisibility(visible = hasSigningAlgorithmOptions) {
    ChipDropdownMenu(
      modifier = Modifier.padding(top = 12.dp),
      options = signingAlgorithmOptions,
      label = stringResource(R.string.authentication_signing_algorithm),
      labelPosition = LabelPosition.Top,
      initialValue = settings.tokenSigningAlgorithm,
      enabled = enabled,
      onClick = { selected -> update { it.copy(tokenSigningAlgorithm = selected) } },
    )
  }
  AnimatedVisibility(visible = !hasSigningAlgorithmOptions) {
    MyOutlinedTextField(
      modifier = Modifier.padding(top = 12.dp),
      value = settings.tokenSigningAlgorithm,
      onValueChange = { changed ->
        update { openId -> openId.copy(tokenSigningAlgorithm = changed) }
      },
      label = stringResource(R.string.authentication_signing_algorithm),
      enabled = enabled,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
  }
  AnimatedVisibility(visible = hasSigningAlgorithmOptions) {
    Text(
      text =
        stringResource(
          R.string.authentication_openid_discovery_algorithms_value,
          signingAlgorithmOptions.joinToString(),
        ),
      modifier = Modifier.padding(top = 8.dp),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
  AnimatedVisibility(visible = discoveryFailureState != null) {
    Text(
      text =
        discoveryFailureState?.message
          ?: stringResource(R.string.authentication_openid_discovery_failed),
      modifier =
        Modifier.padding(top = 8.dp).semantics {
          liveRegion = LiveRegionMode.Polite
        },
      color = MaterialTheme.colorScheme.error,
    )
  }

  Spacer(modifier = Modifier.height(20.dp))
  TextTitleMedium(text = stringResource(R.string.authentication_callbacks))
  Text(
    text = stringResource(R.string.authentication_mobile_redirect_uris_hint),
    modifier = Modifier.padding(top = 8.dp),
    style = MaterialTheme.typography.bodyMedium,
  )
  var newMobileRedirectUri by remember { mutableStateOf("") }
  settings.mobileRedirectUris.forEachIndexed { index, uri ->
    val removeLabel =
      stringResource(R.string.authentication_remove_mobile_redirect_uri_number, index + 1)
    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
      verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
      MyOutlinedTextField(
        modifier = Modifier.weight(1f),
        value = uri,
        onValueChange = { changed ->
          onEvent(AuthenticationSettingsEvent.UpdateMobileRedirectUri(index, changed))
        },
        label = stringResource(R.string.authentication_mobile_redirect_uri_number, index + 1),
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
      )
      TextButton(
        modifier =
          Modifier.padding(start = 8.dp).semantics {
            contentDescription = removeLabel
          },
        enabled = enabled,
        onClick = { onEvent(AuthenticationSettingsEvent.RemoveMobileRedirectUri(index)) },
      ) {
        Text(stringResource(R.string.authentication_remove_mobile_redirect_uri))
      }
    }
  }
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
  ) {
    MyOutlinedTextField(
      modifier = Modifier.weight(1f),
      value = newMobileRedirectUri,
      onValueChange = { newMobileRedirectUri = it },
      label = stringResource(R.string.authentication_mobile_redirect_uri_new),
      enabled = enabled,
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    Button(
      modifier = Modifier.padding(start = 8.dp),
      enabled = enabled && newMobileRedirectUri.isNotBlank(),
      onClick = {
        onEvent(AuthenticationSettingsEvent.AddMobileRedirectUri(newMobileRedirectUri))
        newMobileRedirectUri = ""
      },
    ) {
      Text(stringResource(R.string.authentication_add_mobile_redirect_uri))
    }
  }
  ChipDropdownMenu(
    modifier = Modifier.padding(top = 12.dp),
    options = callbackSubfolderOptions,
    label = stringResource(R.string.authentication_callback_subfolder),
    labelPosition = LabelPosition.Top,
    initialValue = settings.subfolderForRedirectUrls,
    enabled = enabled,
    onClick = { selected -> onEvent(AuthenticationSettingsEvent.SetCallbackSubfolder(selected)) },
  )
  val callbackUrls = settings.callbackUrls(serverBaseUrl)
  SummaryRow(stringResource(R.string.authentication_web_callback_url), callbackUrls.web)
  SummaryRow(stringResource(R.string.authentication_mobile_callback_url), callbackUrls.mobile)

  Spacer(modifier = Modifier.height(20.dp))
  TextTitleMedium(text = stringResource(R.string.authentication_user_mapping))
  MyOutlinedTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = settings.buttonText,
    onValueChange = { changed -> update { it.copy(buttonText = changed) } },
    label = stringResource(R.string.authentication_button_text),
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
  )
  val matchExistingNoneLabel = stringResource(R.string.authentication_match_existing_none)
  val matchExistingEmailLabel = stringResource(R.string.authentication_match_existing_email)
  val matchExistingUsernameLabel = stringResource(R.string.authentication_match_existing_username)
  MySegmentedButton(
    modifier = Modifier.padding(top = 12.dp),
    options = OPENID_MATCH_EXISTING_BY_OPTIONS,
    selectedValue = settings.matchExistingBy,
    label = stringResource(R.string.authentication_match_existing_by),
    optionLabel = { value ->
      when (value) {
        "email" -> matchExistingEmailLabel
        "username" -> matchExistingUsernameLabel
        else -> matchExistingNoneLabel
      }
    },
    enabled = enabled,
    onClick = { selected -> update { it.copy(matchExistingBy = selected) } },
  )
  AnimatedVisibility(visible = invalidExistingUserMatching) {
    Text(
      text = stringResource(R.string.authentication_validation_match_existing_by),
      modifier = Modifier.padding(top = 4.dp),
      color = MaterialTheme.colorScheme.error,
    )
  }
  MySwitch(
    modifier = Modifier.padding(top = 12.dp),
    title = stringResource(R.string.authentication_auto_launch),
    checked = settings.autoLaunch,
    contentDescription = stringResource(R.string.authentication_auto_launch),
    enabled = enabled,
    onCheckedChange = { checked -> update { it.copy(autoLaunch = checked) } },
  )
  Text(
    text = stringResource(R.string.authentication_auto_launch_hint),
    modifier = Modifier.padding(top = 4.dp),
    style = MaterialTheme.typography.bodySmall,
  )
  MySwitch(
    modifier = Modifier.padding(top = 12.dp),
    title = stringResource(R.string.authentication_auto_register),
    checked = settings.autoRegister,
    contentDescription = stringResource(R.string.authentication_auto_register),
    enabled = enabled,
    onCheckedChange = { checked -> update { it.copy(autoRegister = checked) } },
  )
  Text(
    text = stringResource(R.string.authentication_auto_register_hint),
    modifier = Modifier.padding(top = 4.dp),
    style = MaterialTheme.typography.bodySmall,
  )
  MyOutlinedTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = settings.groupClaim,
    onValueChange = { changed -> update { it.copy(groupClaim = changed) } },
    label = stringResource(R.string.authentication_group_claim),
    supportingText = stringResource(R.string.authentication_group_claim_hint),
    isError = AuthenticationSettingsValidationError.InvalidGroupClaim in validationErrors,
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
  )
  MyOutlinedTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = settings.advancedPermsClaim,
    onValueChange = { changed -> update { it.copy(advancedPermsClaim = changed) } },
    label = stringResource(R.string.authentication_advanced_permissions_claim),
    supportingText = stringResource(R.string.authentication_advanced_permissions_claim_hint),
    isError =
      AuthenticationSettingsValidationError.InvalidAdvancedPermissionsClaim in validationErrors,
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
  )
  TextTitleMedium(
    text = stringResource(R.string.authentication_sample_permissions),
    modifier = Modifier.padding(top = 12.dp),
  )
  Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
    SelectionContainer {
      Text(
        text = settings.samplePermissions,
        modifier = Modifier.padding(16.dp),
        style = MaterialTheme.typography.bodyMedium,
      )
    }
  }
}

@Composable
private fun OpenIdEndpointField(
  label: String,
  value: String,
  enabled: Boolean,
  onValueChange: (String) -> Unit,
) {
  MyOutlinedTextField(
    modifier = Modifier.padding(top = 12.dp),
    value = value,
    onValueChange = onValueChange,
    label = label,
    enabled = enabled,
    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
  )
}

@Composable
private fun HtmlMessagePreview(message: String) {
  val linkColor = MaterialTheme.colorScheme.primary
  val linkStyles =
    remember(linkColor) {
      TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = linkColor))
    }
  Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
    Text(
      text = AnnotatedString.fromHtml(message, linkStyles = linkStyles),
      modifier = Modifier.padding(16.dp),
    )
  }
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

@Composable
private fun AuthenticationSettingsValidationError.toDisplayText(): String =
  when (this) {
    AuthenticationSettingsValidationError.NoLoginMethod ->
      stringResource(R.string.authentication_validation_no_login_method)

    AuthenticationSettingsValidationError.OpenIdConfigurationIncomplete ->
      stringResource(R.string.authentication_validation_openid_incomplete)

    AuthenticationSettingsValidationError.InvalidExistingUserMatching ->
      stringResource(R.string.authentication_validation_match_existing_by)

    AuthenticationSettingsValidationError.InvalidGroupClaim ->
      stringResource(R.string.authentication_validation_group_claim)

    AuthenticationSettingsValidationError.InvalidAdvancedPermissionsClaim ->
      stringResource(R.string.authentication_validation_advanced_permissions_claim)

    AuthenticationSettingsValidationError.InvalidMobileRedirectUri ->
      stringResource(R.string.authentication_validation_mobile_redirect_uri)

    AuthenticationSettingsValidationError.WildcardMobileRedirectUriMustBeSoleEntry ->
      stringResource(R.string.authentication_validation_mobile_redirect_wildcard)

    AuthenticationSettingsValidationError.InvalidCallbackSubfolder ->
      stringResource(R.string.authentication_validation_callback_subfolder)
  }

@Composable
private fun confirmationText(confirmation: AuthenticationSettingsConfirmation?): String =
  when (confirmation) {
    AuthenticationSettingsConfirmation.DisablePasswordSignIn ->
      stringResource(R.string.authentication_disable_password_confirm)

    AuthenticationSettingsConfirmation.RemoveShelfDroidCallback ->
      stringResource(R.string.authentication_remove_shelf_callback_confirm)

    AuthenticationSettingsConfirmation.UseWildcardMobileRedirect ->
      stringResource(R.string.authentication_wildcard_redirect_confirm)

    AuthenticationSettingsConfirmation.LeaveWithUnsavedChanges ->
      stringResource(R.string.authentication_unsaved_changes_confirm)

    null -> ""
  }

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsLoadingPreview() {
  PreviewWrapper(dynamicColor = false) { AuthenticationSettingsContent() }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsReadyPreview() {
  val settings = authenticationPreviewSettings()
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState = authenticationPreviewUiState(settings),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsFailurePreview() {
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state =
        AuthenticationSettingsState.Failure(stringResource(R.string.server_could_not_be_reached))
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsDirtyPreview() {
  val saved = authenticationPreviewSettings()
  val draft = saved.copy(customMessage = "<p>Updated</p>", customMessageEnabled = true)
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(draft),
      uiState = authenticationPreviewUiState(saved, draft),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsInvalidPreview() {
  val settings = authenticationPreviewSettings().copy(activeLoginMethods = emptyList())
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState = authenticationPreviewUiState(settings, settings),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsSavingPreview() {
  val settings = authenticationPreviewSettings()
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState =
        authenticationPreviewUiState(
          settings,
          settings.copy(customMessage = "<p>Saving…</p>"),
          apiState = AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Save),
        ),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsDiscoveryPreview() {
  val settings = authenticationPreviewSettings()
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState =
        authenticationPreviewUiState(
          settings,
          settings,
          apiState =
            AuthenticationSettingsApiState.Loading(AuthenticationSettingsOperation.Discovery),
        ),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsDiscoveryFailurePreview() {
  val settings = authenticationPreviewSettings()
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState =
        authenticationPreviewUiState(
          settings,
          settings,
          apiState =
            AuthenticationSettingsApiState.Failure(
              AuthenticationSettingsOperation.Discovery,
              "The provider did not return metadata.",
            ),
        ),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsPasswordOnlyPreview() {
  val settings =
    authenticationPreviewSettings().copy(activeLoginMethods = listOf(LoginMethod.Local))
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState = authenticationPreviewUiState(settings),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun AuthenticationSettingsOpenIdOnlyPreview() {
  val settings =
    authenticationPreviewSettings().copy(activeLoginMethods = listOf(LoginMethod.OpenId))
  PreviewWrapper(dynamicColor = false) {
    AuthenticationSettingsContent(
      state = AuthenticationSettingsState.Ready(settings),
      uiState = authenticationPreviewUiState(settings),
    )
  }
}

private fun authenticationPreviewSettings(): AuthenticationSettingsSummary =
  AuthenticationSettingsSummary(
    customMessageEnabled = true,
    customMessage = "<p>Welcome to ShelfDroid</p>",
    activeLoginMethods = listOf(LoginMethod.Local, LoginMethod.OpenId),
    openId =
      OpenIdSettingsSummary(
        issuerUrl = "https://issuer.example.com",
        authorizationUrl = "https://issuer.example.com/authorize",
        tokenUrl = "https://issuer.example.com/token",
        userInfoUrl = "https://issuer.example.com/userinfo",
        jwksUrl = "https://issuer.example.com/jwks",
        clientId = "shelfdroid",
        clientSecret = "secret-value",
        tokenSigningAlgorithm = "RS256",
        mobileRedirectUris = listOf("audiobookshelf://oauth"),
        matchExistingBy = "email",
      ),
  )

private fun authenticationPreviewUiState(
  saved: AuthenticationSettingsSummary,
  draft: AuthenticationSettingsSummary = saved,
  apiState: AuthenticationSettingsApiState = AuthenticationSettingsApiState.Idle,
): AuthenticationSettingsUiState =
  AuthenticationSettingsUiState(
    state = AuthenticationSettingsState.Ready(draft),
    savedSettings = saved,
    draftSettings = draft,
    apiState = apiState,
    validation = draft.validation(),
  )
