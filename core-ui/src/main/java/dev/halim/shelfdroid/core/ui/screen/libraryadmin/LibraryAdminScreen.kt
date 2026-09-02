@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canDelete
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canReorder
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canStartMatch
import dev.halim.shelfdroid.core.data.screen.libraryadmin.canStartScan
import dev.halim.shelfdroid.core.data.screen.libraryadmin.taskForLibrary
import dev.halim.shelfdroid.core.navigation.LibraryChangedNavResult
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DeleteConfirmationDialog
import dev.halim.shelfdroid.core.ui.components.MyTextButtonRetry
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdminScreen(
  viewModel: LibraryAdminViewModel = hiltViewModel(),
  onCreateLibraryClicked: () -> Unit = {},
  onEditLibraryClicked: (String) -> Unit = {},
  collectNavResultEvent: Boolean = false,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val notification = uiState.taskNotification
  val notificationPresentation = notification?.let { taskPresentation(it.action, it.status) }
  val notificationMessage = notificationPresentation?.let { stringResource(it.statusLabel) }
  LaunchedEffect(notification?.taskId) {
    if (notificationMessage != null) {
      snackbarHostState.showSnackbar(notificationMessage)
      viewModel.consumeTaskNotification()
    }
  }
  if (collectNavResultEvent) {
    ResultEffect<LibraryChangedNavResult> { viewModel.onEvent(LibraryAdminEvent.Refresh) }
  }
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
    LibraryAdminContent(
      modifier = Modifier.padding(paddingValues),
      uiState = uiState,
      onEvent = viewModel::onEvent,
      onCreateLibraryClicked = onCreateLibraryClicked,
      onEditLibraryClicked = onEditLibraryClicked,
    )
  }
}

@Composable
internal fun LibraryAdminContent(
  modifier: Modifier = Modifier,
  uiState: LibraryAdminUiState = LibraryAdminUiState(),
  onEvent: (LibraryAdminEvent) -> Unit = {},
  onCreateLibraryClicked: () -> Unit = {},
  onEditLibraryClicked: (String) -> Unit = {},
) {
  Column(modifier = modifier.fillMaxSize()) {
    if (uiState.state is GenericState.Loading || uiState.isRefreshing) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    uiState.scanError?.let { error ->
      Text(
        text = libraryAdminErrorText(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
    }
    uiState.matchError?.let { error ->
      Text(
        text = libraryAdminErrorText(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
    }
    uiState.taskSyncError?.let { error ->
      Text(
        text = libraryAdminErrorText(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
    }
    uiState.deleteError?.let { error ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = libraryAdminErrorText(error),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onEvent(LibraryAdminEvent.RetryDeleteLibrary) }) {
          Text(stringResource(R.string.retry))
        }
      }
    }
    uiState.deleteSyncError?.let { error ->
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = libraryAdminErrorText(error),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onEvent(LibraryAdminEvent.RetryDeleteSynchronization) }) {
          Text(stringResource(R.string.library_delete_retry_sync))
        }
      }
    }

    PullToRefreshBox(
      modifier = Modifier.fillMaxWidth().weight(1f),
      isRefreshing = false,
      onRefresh = { onEvent(LibraryAdminEvent.Refresh) },
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
      ) {
        when (val state = uiState.state) {
          GenericState.Idle -> Unit

          GenericState.Loading -> Unit

          is GenericState.Failure -> {
            item(key = "failure") {
              MyTextButtonRetry(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                message =
                  state.errorMessage ?: stringResource(R.string.server_could_not_be_reached),
                onRetry = { onEvent(LibraryAdminEvent.Refresh) },
              )
            }
          }

          GenericState.Success -> {
            if (uiState.libraries.isEmpty()) {
              item(key = "empty") {
                Text(
                  text = stringResource(R.string.library_admin_empty),
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  style = MaterialTheme.typography.titleLarge,
                )
              }
            } else {
              itemsIndexed(uiState.libraries, key = { _, library -> library.id }) { index, library
                ->
                val reorderEnabled = uiState.canReorder(library.id)
                LibraryAdminItem(
                  library = library,
                  reorderEnabled = reorderEnabled,
                  scanEnabled = uiState.canStartScan(library.id),
                  matchEnabled = uiState.canStartMatch(library.id),
                  deleteEnabled = uiState.canDelete(library.id),
                  onScan = { onEvent(LibraryAdminEvent.StartScan(library.id)) },
                  onMatch = { onEvent(LibraryAdminEvent.StartMatch(library.id)) },
                  onDelete = { onEvent(LibraryAdminEvent.RequestDeleteLibrary(library.id)) },
                  onEdit = { onEditLibraryClicked(library.id) },
                  task = uiState.taskForLibrary(library.id),
                  onRetrySynchronization = { taskId ->
                    onEvent(LibraryAdminEvent.RetryTaskSynchronization(taskId))
                  },
                  onDragMove = { delta ->
                    onEvent(LibraryAdminEvent.MoveLibrary(library.id, delta))
                  },
                )
              }
            }
          }
        }
      }
      if (uiState.reorderError != null) {
        Text(
          text = stringResource(R.string.library_reorder_failed),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
      uiState.reorderSyncError?.let { error ->
        Row(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = libraryAdminErrorText(error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = { onEvent(LibraryAdminEvent.RetryReorderSynchronization) }) {
            Text(stringResource(R.string.library_reorder_retry_sync))
          }
        }
      }
    }

    TextButton(
      onClick = onCreateLibraryClicked,
      modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
      Text(stringResource(R.string.create_library))
    }
    Spacer(modifier = Modifier.height(16.dp))

    val confirmationLibrary =
      uiState.libraries.firstOrNull { it.id == uiState.deleteConfirmationLibraryId }
    if (confirmationLibrary != null) {
      DeleteConfirmationDialog(
        title = stringResource(R.string.delete_library_title, confirmationLibrary.name),
        message = stringResource(R.string.delete_library_confirmation),
        onConfirm = { onEvent(LibraryAdminEvent.ConfirmDeleteLibrary) },
        onDismiss = { onEvent(LibraryAdminEvent.CancelDeleteLibrary) },
      )
    }
  }
}

@Composable
private fun libraryAdminErrorText(error: LibraryAdminError): String =
  when (error) {
    is LibraryAdminError.SafeMessage -> error.message
    LibraryAdminError.GenericScanStart -> stringResource(R.string.library_scan_start_failed)

    LibraryAdminError.GenericMatchStart -> stringResource(R.string.library_match_start_failed)

    LibraryAdminError.GenericSynchronization -> stringResource(R.string.library_scan_sync_failed)

    LibraryAdminError.GenericDelete -> stringResource(R.string.delete_library_failed)
    LibraryAdminError.GenericDeleteSynchronization ->
      stringResource(R.string.library_delete_sync_failed)

    LibraryAdminError.GenericReorderSynchronization ->
      stringResource(R.string.library_reorder_sync_failed)
  }

@ShelfDroidPreview
@Composable
private fun LibraryAdminContentPreview() {
  PreviewWrapper {
    LibraryAdminContent(
      uiState =
        LibraryAdminUiState(
          state = GenericState.Success,
          isRefreshing = false,
          libraries =
            listOf(
              LibraryAdminLibrary(
                id = "book-library",
                name = "Books",
                mediaType = MediaType.BOOK,
                displayOrder = 0,
              ),
              LibraryAdminLibrary(
                id = "podcast-library",
                name = "Podcasts",
                mediaType = MediaType.PODCAST,
                displayOrder = 1,
              ),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdminFailurePreview() {
  PreviewWrapper {
    LibraryAdminContent(
      uiState =
        LibraryAdminUiState(
          state = GenericState.Failure("The server could not be reached."),
          isRefreshing = false,
        )
    )
  }
}
