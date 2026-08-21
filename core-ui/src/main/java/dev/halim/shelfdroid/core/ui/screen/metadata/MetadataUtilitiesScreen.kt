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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.TagManagementApiState
import dev.halim.shelfdroid.core.data.metadata.TagManagementDialog
import dev.halim.shelfdroid.core.data.metadata.TagManagementUiState
import dev.halim.shelfdroid.core.data.metadata.tagRenameCollision
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar

@Composable
fun MetadataUtilitiesHubScreen(
  onTagsClicked: () -> Unit,
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
    TextButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_custom_providers))
    }
    Text(stringResource(R.string.metadata_not_available_yet))
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
