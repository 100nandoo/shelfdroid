package dev.halim.shelfdroid.core.ui.screen.metadata

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProvider
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementApiState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementDialog
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderManagementUiState
import dev.halim.shelfdroid.core.data.metadata.CustomMetadataProviderOperation
import dev.halim.shelfdroid.core.data.metadata.GenreManagementApiState
import dev.halim.shelfdroid.core.data.metadata.GenreManagementDialog
import dev.halim.shelfdroid.core.data.metadata.GenreManagementUiState
import dev.halim.shelfdroid.core.data.metadata.GenreOperation
import dev.halim.shelfdroid.core.data.metadata.genreRenameCollision
import dev.halim.shelfdroid.core.data.metadata.TagManagementApiState
import dev.halim.shelfdroid.core.data.metadata.TagManagementDialog
import dev.halim.shelfdroid.core.data.metadata.TagManagementUiState
import dev.halim.shelfdroid.core.data.metadata.tagRenameCollision
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.PasswordTextField
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar

@Composable
fun MetadataUtilitiesHubScreen(
  onTagsClicked: () -> Unit,
  onGenresClicked: () -> Unit = {},
  onCustomProvidersClicked: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    TextHeadlineSmall(text = stringResource(R.string.library_item_metadata_utilities))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.metadata_utilities_description))
    Spacer(Modifier.height(16.dp))
    Button(onClick = onTagsClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_tags))
    }
    TextButton(onClick = onGenresClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_genres))
    }
    TextButton(onClick = onCustomProvidersClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_custom_providers))
    }
  }
}

@Composable
fun TagManagementScreen(
  viewModel: TagManagementViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  TagManagementSnackbar(uiState, snackbarHostState) { viewModel.onEvent(TagManagementEvent.ClearApiState) }
  TagManagementContent(uiState) { viewModel.onEvent(it) }
}

@Composable
fun GenreManagementScreen(
  viewModel: GenreManagementViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  GenreManagementSnackbar(uiState, snackbarHostState) {
    viewModel.onEvent(GenreManagementEvent.ClearApiState)
  }
  GenreManagementContent(uiState) { viewModel.onEvent(it) }
}

@Composable
fun CustomMetadataProviderManagementScreen(
  viewModel: CustomMetadataProviderManagementViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val createdMessage = stringResource(R.string.metadata_provider_created)
  val deletedMessage = stringResource(R.string.metadata_provider_deleted)
  DisposableEffect(Unit) {
    onDispose {
      viewModel.onEvent(CustomMetadataProviderManagementEvent.ClearSensitiveState)
    }
  }
  LaunchedEffect(uiState.apiState) {
    when (val apiState = uiState.apiState) {
      CustomMetadataProviderManagementApiState.CreateSuccess -> {
        snackbarHostState.showSuccessSnackbar(createdMessage)
        viewModel.onEvent(CustomMetadataProviderManagementEvent.ClearApiState)
      }
      CustomMetadataProviderManagementApiState.DeleteSuccess -> {
        snackbarHostState.showSuccessSnackbar(deletedMessage)
        viewModel.onEvent(CustomMetadataProviderManagementEvent.ClearApiState)
      }
      is CustomMetadataProviderManagementApiState.Failure -> {
        val operation = apiState.operation
        if (!apiState.accessDenied && operation != null) {
          snackbarHostState.showErrorSnackbar(
            customMetadataProviderFailureMessage(operation, apiState.message)
          )
          viewModel.onEvent(CustomMetadataProviderManagementEvent.ClearApiState)
        }
      }
      else -> Unit
    }
  }
  CustomMetadataProviderManagementContent(uiState) { viewModel.onEvent(it) }
}

@Composable
fun CustomMetadataProviderManagementContent(
  uiState: CustomMetadataProviderManagementUiState,
  onEvent: (CustomMetadataProviderManagementEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    TextHeadlineSmall(text = stringResource(R.string.metadata_custom_providers))
    Spacer(Modifier.height(8.dp))
    when {
      (uiState.apiState as? CustomMetadataProviderManagementApiState.Failure)?.accessDenied == true ->
        ProviderErrorState(
          message = stringResource(R.string.metadata_provider_access_denied),
          retryable = false,
          onEvent = onEvent,
        )
      uiState.state is GenericState.Loading -> {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(stringResource(R.string.metadata_provider_loading), Modifier.padding(vertical = 16.dp))
      }
      else -> {
        ProviderCreateForm(uiState, onEvent)
        Spacer(Modifier.height(16.dp))
        if (uiState.state is GenericState.Failure) {
          ProviderErrorState(
            message = stringResource(R.string.metadata_provider_load_failed),
            retryable = true,
            onEvent = onEvent,
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
              CustomMetadataProviderRow(provider, uiState, onEvent)
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
    is CustomMetadataProviderManagementDialog.Delete ->
      CustomMetadataProviderDeleteDialog(dialog.provider, onEvent)
    null -> Unit
  }
}

@Composable
private fun ProviderCreateForm(
  uiState: CustomMetadataProviderManagementUiState,
  onEvent: (CustomMetadataProviderManagementEvent) -> Unit,
) {
  val enabled = !uiState.isMutating
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    MyOutlinedTextField(
      enabled = enabled,
      value = uiState.nameDraft,
      onValueChange = { onEvent(CustomMetadataProviderManagementEvent.UpdateName(it)) },
      label = stringResource(R.string.metadata_provider_name),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )
    MyOutlinedTextField(
      enabled = enabled,
      value = uiState.urlDraft,
      onValueChange = { onEvent(CustomMetadataProviderManagementEvent.UpdateUrl(it)) },
      label = stringResource(R.string.metadata_provider_url),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    PasswordTextField(
      enabled = enabled,
      value = uiState.authHeaderDraft,
      onValueChange = { onEvent(CustomMetadataProviderManagementEvent.UpdateAuthHeader(it)) },
      label = stringResource(R.string.metadata_provider_auth_header),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
      visible = uiState.authHeaderVisible,
      onVisibilityChange = {
        onEvent(CustomMetadataProviderManagementEvent.SetAuthHeaderVisible(it))
      },
      showVisibilityDescription = stringResource(R.string.metadata_provider_show_auth_header),
      hideVisibilityDescription = stringResource(R.string.metadata_provider_hide_auth_header),
    )
    Text(stringResource(R.string.metadata_provider_books_only))
    Button(
      enabled = enabled && uiState.nameDraft.isNotBlank() && uiState.urlDraft.isNotBlank(),
      onClick = { onEvent(CustomMetadataProviderManagementEvent.SubmitCreate) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.metadata_provider_add))
    }
  }
}

@Composable
private fun CustomMetadataProviderRow(
  provider: CustomMetadataProvider,
  uiState: CustomMetadataProviderManagementUiState,
  onEvent: (CustomMetadataProviderManagementEvent) -> Unit,
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
        onClick = { onEvent(CustomMetadataProviderManagementEvent.BeginDelete(provider)) },
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
          onEvent(CustomMetadataProviderManagementEvent.SetProviderVisible(provider.id, it))
        },
        showVisibilityDescription = stringResource(R.string.metadata_provider_show_auth_header),
        hideVisibilityDescription = stringResource(R.string.metadata_provider_hide_auth_header),
      )
    } else {
      Text(stringResource(R.string.metadata_provider_no_auth_header))
    }
  }
}

@Composable
private fun ProviderErrorState(
  message: String,
  retryable: Boolean,
  onEvent: (CustomMetadataProviderManagementEvent) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(message)
    if (retryable) {
      TextButton(onClick = { onEvent(CustomMetadataProviderManagementEvent.Retry) }) {
        Text(stringResource(R.string.retry))
      }
    }
  }
}

@Composable
private fun CustomMetadataProviderDeleteDialog(
  provider: CustomMetadataProvider,
  onEvent: (CustomMetadataProviderManagementEvent) -> Unit,
) {
  AlertDialog(
    onDismissRequest = { onEvent(CustomMetadataProviderManagementEvent.DismissDialog) },
    title = { Text(stringResource(R.string.metadata_provider_delete)) },
    text = { Text(stringResource(R.string.metadata_provider_delete_confirm, provider.name)) },
    confirmButton = {
      Button(onClick = { onEvent(CustomMetadataProviderManagementEvent.ConfirmDelete) }) {
        Text(stringResource(R.string.delete))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(CustomMetadataProviderManagementEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun GenreManagementSnackbar(
  uiState: GenreManagementUiState,
  snackbarHostState: SnackbarHostState,
  clear: () -> Unit,
) {
  val renameSuccess = uiState.apiState as? GenreManagementApiState.RenameSuccess
  val deleteSuccess = uiState.apiState as? GenreManagementApiState.DeleteSuccess
  val failure = uiState.apiState as? GenreManagementApiState.Failure
  val renameMessage =
    renameSuccess?.let {
      if (it.merged) stringResource(R.string.genre_renamed_merged, it.updatedItemCount)
      else stringResource(R.string.genre_renamed, it.updatedItemCount)
    }
  val deleteMessage = deleteSuccess?.let { stringResource(R.string.genre_deleted, it.updatedItemCount) }
  val failureMessage =
    when (failure?.operation) {
      GenreOperation.Rename -> stringResource(R.string.genre_rename_failed)
      GenreOperation.Delete -> stringResource(R.string.genre_delete_failed)
      null -> failure?.message ?: stringResource(R.string.genre_mutation_failed)
    }
  LaunchedEffect(uiState.apiState) {
    when {
      renameMessage != null -> snackbarHostState.showSuccessSnackbar(renameMessage)
      deleteMessage != null -> snackbarHostState.showSuccessSnackbar(deleteMessage)
      failure != null && !failure.accessDenied ->
        snackbarHostState.showErrorSnackbar(failureMessage)
    }
    if (renameSuccess != null || deleteSuccess != null || failure != null) clear()
  }
}

@Composable
private fun GenreManagementContent(
  uiState: GenreManagementUiState,
  onEvent: (GenreManagementEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (
      uiState.state is GenericState.Loading ||
        uiState.apiState is GenreManagementApiState.Mutating
    ) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    when {
      (uiState.apiState as? GenreManagementApiState.Failure)?.accessDenied == true ->
        GenreErrorState(
          stringResource(R.string.genre_access_denied),
          retryable = false,
          onEvent = onEvent,
        )
      uiState.state is GenericState.Failure ->
        GenreErrorState(
          stringResource(R.string.genre_load_failed),
          retryable = true,
          onEvent = onEvent,
        )
      uiState.state is GenericState.Loading ->
        Text(stringResource(R.string.genre_loading), Modifier.padding(16.dp))
      uiState.genres.isEmpty() ->
        Text(stringResource(R.string.genre_empty), Modifier.padding(16.dp))
      else ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          items(uiState.genres, key = { it }) { genre ->
            GenreRow(
              genre = genre,
              enabled = !uiState.isMutating,
              onRename = { onEvent(GenreManagementEvent.BeginRename(genre)) },
              onDelete = { onEvent(GenreManagementEvent.BeginDelete(genre)) },
            )
          }
        }
    }
  }

  when (val dialog = uiState.dialog) {
    is GenreManagementDialog.Rename -> GenreRenameDialog(uiState, dialog.genre, onEvent)
    is GenreManagementDialog.Delete -> GenreDeleteDialog(dialog.genre, onEvent)
    null -> Unit
  }
}

@Composable
private fun GenreErrorState(
  message: String,
  retryable: Boolean,
  onEvent: (GenreManagementEvent) -> Unit,
) {
  Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(message)
    if (retryable) {
      TextButton(onClick = { onEvent(GenreManagementEvent.Retry) }) {
        Text(stringResource(R.string.retry))
      }
    }
  }
}

@Composable
private fun GenreRow(genre: String, enabled: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(genre, Modifier.weight(1f).padding(vertical = 12.dp))
    IconButton(onClick = onRename, enabled = enabled) {
      Icon(
        painter = painterResource(R.drawable.edit),
        contentDescription = stringResource(R.string.rename_genre_action),
      )
    }
    IconButton(onClick = onDelete, enabled = enabled) {
      Icon(
        painter = painterResource(R.drawable.delete),
        contentDescription = stringResource(R.string.delete_genre_action),
      )
    }
  }
}

@Composable
private fun GenreRenameDialog(
  uiState: GenreManagementUiState,
  currentGenre: String,
  onEvent: (GenreManagementEvent) -> Unit,
) {
  val target = uiState.renameDraft.trim()
  val collision = genreRenameCollision(currentGenre, target, uiState.genres)
  val blank = target.isBlank()
  AlertDialog(
    onDismissRequest = { onEvent(GenreManagementEvent.DismissDialog) },
    title = { Text(stringResource(R.string.genre_rename)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = uiState.renameDraft,
          onValueChange = { onEvent(GenreManagementEvent.UpdateRenameDraft(it)) },
          label = { Text(stringResource(R.string.genre_new_name)) },
          isError = blank,
          supportingText = { if (blank) Text(stringResource(R.string.genre_name_required)) },
          singleLine = true,
        )
        if (collision.exact) Text(stringResource(R.string.genre_exact_collision, target))
        else if (collision.caseOnly) Text(stringResource(R.string.genre_case_collision))
        Text(stringResource(R.string.genre_rename_confirm, currentGenre, target))
      }
    },
    confirmButton = {
      Button(onClick = { onEvent(GenreManagementEvent.ConfirmRename) }, enabled = !blank) {
        Text(stringResource(R.string.genre_rename))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(GenreManagementEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun GenreDeleteDialog(genre: String, onEvent: (GenreManagementEvent) -> Unit) {
  AlertDialog(
    onDismissRequest = { onEvent(GenreManagementEvent.DismissDialog) },
    title = { Text(stringResource(R.string.genre_delete)) },
    text = { Text(stringResource(R.string.genre_delete_confirm, genre)) },
    confirmButton = {
      Button(onClick = { onEvent(GenreManagementEvent.ConfirmDelete) }) {
        Text(stringResource(R.string.delete))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(GenreManagementEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun TagManagementSnackbar(
  uiState: TagManagementUiState,
  snackbarHostState: SnackbarHostState,
  clear: () -> Unit,
) {
  val renameSuccess = uiState.apiState as? TagManagementApiState.RenameSuccess
  val deleteSuccess = uiState.apiState as? TagManagementApiState.DeleteSuccess
  val failure = uiState.apiState as? TagManagementApiState.Failure
  val renameMessage =
    renameSuccess?.let {
      if (it.merged) stringResource(R.string.tag_renamed_merged, it.updatedItemCount)
      else stringResource(R.string.tag_renamed, it.updatedItemCount)
    }
  val deleteMessage = deleteSuccess?.let { stringResource(R.string.tag_deleted, it.updatedItemCount) }
  val failureMessage = failure?.message ?: stringResource(R.string.tag_mutation_failed)
  LaunchedEffect(uiState.apiState) {
    when {
      renameMessage != null -> snackbarHostState.showSuccessSnackbar(renameMessage)
      deleteMessage != null -> snackbarHostState.showSuccessSnackbar(deleteMessage)
      failure != null && !failure.accessDenied ->
        snackbarHostState.showErrorSnackbar(failureMessage)
    }
    if (renameSuccess != null || deleteSuccess != null || failure != null) clear()
  }
}

@Composable
private fun TagManagementContent(
  uiState: TagManagementUiState,
  onEvent: (TagManagementEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.state is GenericState.Loading || uiState.apiState is TagManagementApiState.Mutating) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    when {
      (uiState.apiState as? TagManagementApiState.Failure)?.accessDenied == true ->
        ErrorState(stringResource(R.string.tag_access_denied), retryable = false, onEvent = onEvent)
      uiState.state is GenericState.Failure ->
        ErrorState(stringResource(R.string.tag_load_failed), retryable = true, onEvent = onEvent)
      uiState.state is GenericState.Loading -> Text(stringResource(R.string.tag_loading), Modifier.padding(16.dp))
      uiState.tags.isEmpty() -> Text(stringResource(R.string.tag_empty), Modifier.padding(16.dp))
      else ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
          items(uiState.tags, key = { it }) { tag ->
            TagRow(
              tag = tag,
              enabled = !uiState.isMutating,
              onRename = { onEvent(TagManagementEvent.BeginRename(tag)) },
              onDelete = { onEvent(TagManagementEvent.BeginDelete(tag)) },
            )
          }
        }
    }
  }

  when (val dialog = uiState.dialog) {
    is TagManagementDialog.Rename -> RenameDialog(uiState, dialog.tag, onEvent)
    is TagManagementDialog.Delete -> DeleteDialog(dialog.tag, onEvent)
    null -> Unit
  }
}

@Composable
private fun ErrorState(
  message: String,
  retryable: Boolean,
  onEvent: (TagManagementEvent) -> Unit,
) {
  Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(message)
    if (retryable) {
      TextButton(onClick = { onEvent(TagManagementEvent.Retry) }) {
        Text(stringResource(R.string.retry))
      }
    }
  }
}

@Composable
private fun TagRow(tag: String, enabled: Boolean, onRename: () -> Unit, onDelete: () -> Unit) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(tag, Modifier.weight(1f).padding(vertical = 12.dp))
    IconButton(onClick = onRename, enabled = enabled) {
      Icon(
        painter = painterResource(R.drawable.edit),
        contentDescription = stringResource(R.string.rename_tag_action),
      )
    }
    IconButton(onClick = onDelete, enabled = enabled) {
      Icon(
        painter = painterResource(R.drawable.delete),
        contentDescription = stringResource(R.string.delete_tag_action),
      )
    }
  }
}

@Composable
private fun RenameDialog(
  uiState: TagManagementUiState,
  currentTag: String,
  onEvent: (TagManagementEvent) -> Unit,
) {
  val target = uiState.renameDraft.trim()
  val collision = tagRenameCollision(currentTag, target, uiState.tags)
  val blank = target.isBlank()
  AlertDialog(
    onDismissRequest = { onEvent(TagManagementEvent.DismissDialog) },
    title = { Text(stringResource(R.string.tag_rename)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = uiState.renameDraft,
          onValueChange = { onEvent(TagManagementEvent.UpdateRenameDraft(it)) },
          label = { Text(stringResource(R.string.tag_new_name)) },
          isError = blank,
          supportingText = { if (blank) Text(stringResource(R.string.tag_name_required)) },
          singleLine = true,
        )
        if (collision.exact) Text(stringResource(R.string.tag_exact_collision, target))
        else if (collision.caseOnly) Text(stringResource(R.string.tag_case_collision))
        Text(stringResource(R.string.tag_rename_confirm, currentTag, target))
      }
    },
    confirmButton = {
      Button(onClick = { onEvent(TagManagementEvent.ConfirmRename) }, enabled = !blank) {
        Text(stringResource(R.string.tag_rename))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(TagManagementEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun DeleteDialog(tag: String, onEvent: (TagManagementEvent) -> Unit) {
  AlertDialog(
    onDismissRequest = { onEvent(TagManagementEvent.DismissDialog) },
    title = { Text(stringResource(R.string.tag_delete)) },
    text = { Text(stringResource(R.string.tag_delete_confirm, tag)) },
    confirmButton = {
      Button(onClick = { onEvent(TagManagementEvent.ConfirmDelete) }) {
        Text(stringResource(R.string.delete))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(TagManagementEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}
