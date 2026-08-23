package dev.halim.shelfdroid.core.ui.screen.metadata.genre

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.metadata.genre.GenreApiState
import dev.halim.shelfdroid.core.data.metadata.genre.GenreDialog
import dev.halim.shelfdroid.core.data.metadata.genre.GenreOperation
import dev.halim.shelfdroid.core.data.metadata.genre.GenreUiState
import dev.halim.shelfdroid.core.data.metadata.genre.genreRenameCollision
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DeleteConfirmationDialog
import dev.halim.shelfdroid.core.ui.components.MyTextButtonRetry
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.metadata.GenreTagItem

@Composable
fun GenreScreen(
  viewModel: GenreViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  GenreSnackbar(uiState, snackbarHostState) {
    viewModel.onEvent(GenreEvent.ClearApiState)
  }
  GenreContent(uiState) { viewModel.onEvent(it) }
}

@Composable
private fun GenreSnackbar(
  uiState: GenreUiState,
  snackbarHostState: SnackbarHostState,
  clear: () -> Unit,
) {
  val renameSuccess = uiState.apiState as? GenreApiState.RenameSuccess
  val deleteSuccess = uiState.apiState as? GenreApiState.DeleteSuccess
  val failure = uiState.apiState as? GenreApiState.Failure
  val renameMessage = renameSuccess?.let {
    if (it.merged) stringResource(R.string.genre_renamed_merged, it.updatedItemCount)
    else stringResource(R.string.genre_renamed, it.updatedItemCount)
  }
  val deleteMessage = deleteSuccess?.let {
    stringResource(R.string.genre_deleted, it.updatedItemCount)
  }
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
      failure != null -> snackbarHostState.showErrorSnackbar(failureMessage)
    }
    if (renameSuccess != null || deleteSuccess != null || failure != null) clear()
  }
}

@Composable
private fun GenreContent(
  uiState: GenreUiState,
  onEvent: (GenreEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.state is GenericState.Loading || uiState.apiState is GenreApiState.Mutating) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    Box(
      modifier = Modifier.fillMaxWidth().weight(1f),
      contentAlignment = Alignment.BottomStart,
    ) {
      when {
        uiState.state is GenericState.Failure ->
          MyTextButtonRetry(
            message = stringResource(R.string.genre_load_failed),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            onRetry = { onEvent(GenreEvent.Retry) },
          )
        uiState.genres.isEmpty() && uiState.state is GenericState.Success ->
          GenericMessageScreen(stringResource(R.string.empty_type, stringResource(R.string.genres)))

        else ->
          LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.Bottom,
          ) {
            items(uiState.genres, key = { it }) { genre ->
              GenreTagItem(
                label = genre,
                enabled = !uiState.isMutating,
                renameContentDescriptionResId = R.string.rename_genre_action,
                deleteContentDescriptionResId = R.string.delete_genre_action,
                onRename = { onEvent(GenreEvent.BeginRename(genre)) },
                onDelete = { onEvent(GenreEvent.BeginDelete(genre)) },
              )
            }
          }
      }
    }
  }

  when (val dialog = uiState.dialog) {
    is GenreDialog.Rename -> GenreRenameDialog(uiState, dialog.genre, onEvent)
    is GenreDialog.Delete ->
      DeleteConfirmationDialog(
        title = stringResource(R.string.genre_delete),
        message = stringResource(R.string.genre_delete_confirm, dialog.genre),
        onConfirm = { onEvent(GenreEvent.ConfirmDelete) },
        onDismiss = { onEvent(GenreEvent.DismissDialog) },
      )
    null -> Unit
  }
}

@Composable
private fun GenreRenameDialog(
  uiState: GenreUiState,
  currentGenre: String,
  onEvent: (GenreEvent) -> Unit,
) {
  val target = uiState.renameDraft.trim()
  val collision = genreRenameCollision(currentGenre, target, uiState.genres)
  val blank = target.isBlank()
  AlertDialog(
    onDismissRequest = { onEvent(GenreEvent.DismissDialog) },
    title = { Text(stringResource(R.string.genre_rename)) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = uiState.renameDraft,
          onValueChange = { onEvent(GenreEvent.UpdateRenameDraft(it)) },
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
      Button(onClick = { onEvent(GenreEvent.ConfirmRename) }, enabled = !blank) {
        Text(stringResource(R.string.genre_rename))
      }
    },
    dismissButton = {
      TextButton(onClick = { onEvent(GenreEvent.DismissDialog) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@ShelfDroidPreview
@Composable
private fun GenreContentPreview() {
  PreviewWrapper {
    GenreContent(
      uiState =
        GenreUiState(state = GenericState.Success, genres = listOf("Drama", "Science Fiction")),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun GenreContentEmptyPreview() {
  PreviewWrapper {
    GenreContent(
      uiState = GenreUiState(state = GenericState.Success, genres = emptyList()),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun GenreContentFailurePreview() {
  PreviewWrapper {
    GenreContent(
      uiState = GenreUiState(state = GenericState.Failure("Unable to load genres.")),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun GenreRenameDialogPreview() {
  AnimatedPreviewWrapper {
    GenreRenameDialog(
      uiState =
        GenreUiState(
          state = GenericState.Success,
          genres = listOf("Drama", "Science Fiction"),
          renameDraft = "Audio Drama",
        ),
      currentGenre = "Drama",
      onEvent = {},
    )
  }
}
