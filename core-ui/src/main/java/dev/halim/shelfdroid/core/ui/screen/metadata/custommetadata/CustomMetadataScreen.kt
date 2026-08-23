package dev.halim.shelfdroid.core.ui.screen.metadata.custommetadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataApiState
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataDialog
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.custommetadata.CustomMetadataUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DeleteConfirmationDialog
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MyTextButtonRetry
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun CustomMetadataScreen(
  viewModel: CustomMetadataViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val createdMessage = stringResource(R.string.metadata_provider_created)
  val deletedMessage = stringResource(R.string.metadata_provider_deleted)
  val failureMessages =
    CustomMetadataFailureMessages(
      createFailed = stringResource(R.string.metadata_provider_create_failed),
      deleteFailed = stringResource(R.string.metadata_provider_delete_failed),
      providerNameRequired = stringResource(R.string.metadata_provider_name_required),
      providerUrlRequired = stringResource(R.string.metadata_provider_url_required),
    )
  DisposableEffect(Unit) {
    onDispose {
      viewModel.onEvent(CustomMetadataEvent.ClearSensitiveState)
    }
  }
  LaunchedEffect(uiState.apiState) {
    when (val apiState = uiState.apiState) {
      CustomMetadataApiState.CreateSuccess -> {
        snackbarHostState.showSuccessSnackbar(createdMessage)
        viewModel.onEvent(CustomMetadataEvent.ClearApiState)
      }

      CustomMetadataApiState.DeleteSuccess -> {
        snackbarHostState.showSuccessSnackbar(deletedMessage)
        viewModel.onEvent(CustomMetadataEvent.ClearApiState)
      }

      is CustomMetadataApiState.Failure -> {
        val operation = apiState.operation
        if (operation != null) {
          snackbarHostState.showErrorSnackbar(
            customMetadataFailureMessage(
              operation = operation,
              validationError = apiState.validationError,
              detail = apiState.serverDetail,
              messages = failureMessages,
            )
          )
          viewModel.onEvent(CustomMetadataEvent.ClearApiState)
        }
      }

      else -> Unit
    }
  }
  CustomMetadataContent(uiState) { viewModel.onEvent(it) }
}

@Composable
internal fun CustomMetadataContent(
  uiState: CustomMetadataUiState,
  onEvent: (CustomMetadataEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    TextHeadlineSmall(text = stringResource(R.string.metadata_custom_providers))
    Spacer(Modifier.height(8.dp))
    when {
      uiState.state is GenericState.Loading -> {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.metadata_provider_loading), Modifier.padding(vertical = 16.dp))
      }

      else -> {
        ProviderCreateForm(uiState, onEvent)
        Spacer(Modifier.height(16.dp))
        if (uiState.state is GenericState.Failure) {
          MyTextButtonRetry(
            message = stringResource(R.string.metadata_provider_load_failed),
            onRetry = { onEvent(CustomMetadataEvent.Retry) },
          )
        }
        if (uiState.providers.isEmpty()) {
          if (uiState.state !is GenericState.Failure) {
            Text(stringResource(R.string.metadata_provider_empty))
          }
        } else {
          LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            items(uiState.providers, key = { it.id }) { provider ->
              CustomMetadataRow(provider, uiState, onEvent)
            }
          }
        }
      }
    }
    if (uiState.isMutating) {
      LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
    }
  }

  when (val dialog = uiState.dialog) {
    is CustomMetadataDialog.Delete ->
      DeleteConfirmationDialog(
        title = stringResource(R.string.metadata_provider_delete),
        message = stringResource(R.string.metadata_provider_delete_confirm, dialog.provider.name),
        onConfirm = { onEvent(CustomMetadataEvent.ConfirmDelete) },
        onDismiss = { onEvent(CustomMetadataEvent.DismissDialog) },
      )

    null -> Unit
  }
}

@Composable
private fun ProviderCreateForm(
  uiState: CustomMetadataUiState,
  onEvent: (CustomMetadataEvent) -> Unit,
) {
  val enabled = !uiState.isMutating
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MyOutlinedTextField(
      enabled = enabled,
      value = uiState.nameDraft,
      onValueChange = { onEvent(CustomMetadataEvent.UpdateName(it)) },
      label = stringResource(R.string.metadata_provider_name),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
    MyOutlinedTextField(
      enabled = enabled,
      value = uiState.urlDraft,
      onValueChange = { onEvent(CustomMetadataEvent.UpdateUrl(it)) },
      label = stringResource(R.string.metadata_provider_url),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    PasswordTextField(
      enabled = enabled,
      value = uiState.authHeaderDraft,
      onValueChange = { onEvent(CustomMetadataEvent.UpdateAuthHeader(it)) },
      label = stringResource(R.string.metadata_provider_auth_header),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      visible = uiState.authHeaderVisible,
      onVisibilityChange = {
        onEvent(CustomMetadataEvent.SetAuthHeaderVisible(it))
      },
      showVisibilityDescription = stringResource(R.string.metadata_provider_show_auth_header),
      hideVisibilityDescription = stringResource(R.string.metadata_provider_hide_auth_header),
    )
    Button(
      enabled = enabled && uiState.nameDraft.isNotBlank() && uiState.urlDraft.isNotBlank(),
      onClick = { onEvent(CustomMetadataEvent.SubmitCreate) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.metadata_provider_add))
    }
  }
}

@Composable
private fun CustomMetadataRow(
  provider: CustomMetadataProvider,
  uiState: CustomMetadataUiState,
  onEvent: (CustomMetadataEvent) -> Unit,
) {
  val authHeaderValue = provider.authHeaderValue
  Column(modifier = Modifier.fillMaxWidth()) {
    Row(modifier = Modifier.fillMaxWidth()) {
      Column(modifier = Modifier.weight(1f)) {
        Text(provider.name)
        if (provider.url.isNotBlank()) Text(provider.url)
      }
      IconButton(
        enabled = !uiState.isMutating,
        onClick = { onEvent(CustomMetadataEvent.BeginDelete(provider)) },
      ) {
        Icon(
          painter = painterResource(R.drawable.delete),
          contentDescription = stringResource(R.string.metadata_provider_delete_action),
        )
      }
    }
    if (authHeaderValue != null) {
      PasswordTextField(
        readOnly = true,
        value = authHeaderValue,
        onValueChange = {},
        label = stringResource(R.string.metadata_provider_auth_header),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visible = provider.id in uiState.revealedProviderIds,
        onVisibilityChange = {
          onEvent(CustomMetadataEvent.SetProviderVisible(provider.id, it))
        },
        showVisibilityDescription = stringResource(R.string.metadata_provider_show_auth_header),
        hideVisibilityDescription = stringResource(R.string.metadata_provider_hide_auth_header),
      )
    } else {
      Text(stringResource(R.string.metadata_provider_no_auth_header))
    }
  }
}

@ShelfDroidPreview
@Composable
private fun CustomMetadataContentPreview() {
  PreviewWrapper {
    CustomMetadataContent(
      uiState =
        CustomMetadataUiState(
          state = GenericState.Success,
          providers =
            listOf(
              CustomMetadataProvider(
                id = "google-books",
                name = "Google Books",
                url = "https://books.google.com",
              )
            ),
        ),
      onEvent = {},
    )
  }
}
