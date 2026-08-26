@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.result.ResultEffect
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canReorder
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canDelete
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canStartMatch
import dev.halim.shelfdroid.core.data.screen.libraryadministration.canStartScan
import dev.halim.shelfdroid.core.data.screen.libraryadministration.taskForLibrary
import dev.halim.shelfdroid.core.navigation.LibraryCreatedNavResult
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTextButtonRetry
import dev.halim.shelfdroid.core.ui.components.DeleteConfirmationDialog
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdministrationScreen(
  viewModel: LibraryAdministrationViewModel = hiltViewModel(),
  onCreateLibraryClicked: () -> Unit = {},
  collectNavResultEvent: Boolean = false,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  val notification = uiState.taskNotification
  val notificationPresentation =
    notification?.let { serverTaskPresentation(it.action, it.status) }
  val notificationMessage =
    notificationPresentation?.let { stringResource(it.statusLabel) }
  LaunchedEffect(notification?.taskId) {
    if (notificationMessage != null) {
      snackbarHostState.showSnackbar(notificationMessage)
      viewModel.consumeTaskNotification()
    }
  }
  if (collectNavResultEvent) {
    ResultEffect<LibraryCreatedNavResult> { viewModel.onEvent(LibraryAdministrationEvent.Refresh) }
  }
  Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
    LibraryAdministrationContent(
      modifier = Modifier.padding(paddingValues),
      uiState = uiState,
      onEvent = viewModel::onEvent,
      onCreateLibraryClicked = onCreateLibraryClicked,
    )
  }
}

@Composable
internal fun LibraryAdministrationContent(
  modifier: Modifier = Modifier,
  uiState: LibraryAdministrationUiState = LibraryAdministrationUiState(),
  onEvent: (LibraryAdministrationEvent) -> Unit = {},
  onCreateLibraryClicked: () -> Unit = {},
) {
  Column(modifier = modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.library_administration),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = { onEvent(LibraryAdministrationEvent.Refresh) },
        enabled = !uiState.isRefreshing,
      ) {
        Icon(
          painter = painterResource(R.drawable.refresh),
          contentDescription = stringResource(R.string.refresh_libraries),
        )
      }
    }

    Button(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
      onClick = onCreateLibraryClicked,
    ) {
      Text(stringResource(R.string.create_library))
    }

    uiState.scanError?.let { error ->
      Text(
        text = libraryAdministrationErrorText(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
    }
    uiState.matchError?.let { error ->
      Text(
        text = libraryAdministrationErrorText(error),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      )
    }
    uiState.taskSyncError?.let { error ->
      Text(
        text = libraryAdministrationErrorText(error),
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
          text = libraryAdministrationErrorText(error),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onEvent(LibraryAdministrationEvent.RetryDeleteLibrary) }) {
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
          text = libraryAdministrationErrorText(error),
          color = MaterialTheme.colorScheme.error,
          modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { onEvent(LibraryAdministrationEvent.RetryDeleteSynchronization) }) {
          Text(stringResource(R.string.library_delete_retry_sync))
        }
      }
    }

    PullToRefreshBox(
      modifier = Modifier.fillMaxWidth().weight(1f),
      isRefreshing = uiState.isRefreshing,
      onRefresh = { onEvent(LibraryAdministrationEvent.Refresh) },
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
      ) {
        when (val state = uiState.state) {
          GenericState.Idle -> Unit

          GenericState.Loading -> {
            item(key = "loading") {
              Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }

          is GenericState.Failure -> {
            item(key = "failure") {
              MyTextButtonRetry(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                message = state.errorMessage ?: stringResource(R.string.server_could_not_be_reached),
                onRetry = { onEvent(LibraryAdministrationEvent.Refresh) },
              )
            }
          }

          GenericState.Success -> {
            if (uiState.libraries.isEmpty()) {
              item(key = "empty") {
                Text(
                  text = stringResource(R.string.library_administration_empty),
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  style = MaterialTheme.typography.titleLarge,
                )
              }
            } else {
              itemsIndexed(uiState.libraries, key = { _, library -> library.id }) { index, library ->
                val reorderEnabled = uiState.canReorder(library.id)
                LibraryAdministrationItem(
                  library = library,
                  reorderEnabled = reorderEnabled,
                  scanEnabled = uiState.canStartScan(library.id),
                  matchEnabled = uiState.canStartMatch(library.id),
                  deleteEnabled = uiState.canDelete(library.id),
                  onScan = { onEvent(LibraryAdministrationEvent.StartScan(library.id)) },
                  onMatch = { onEvent(LibraryAdministrationEvent.StartMatch(library.id)) },
                  onDelete = { onEvent(LibraryAdministrationEvent.RequestDeleteLibrary(library.id)) },
                  task = uiState.taskForLibrary(library.id),
                  onRetrySynchronization = { taskId ->
                    onEvent(LibraryAdministrationEvent.RetryTaskSynchronization(taskId))
                  },
                  onDragMove = { delta ->
                    onEvent(LibraryAdministrationEvent.MoveLibrary(library.id, delta))
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
            text = libraryAdministrationErrorText(error),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
          )
          TextButton(onClick = { onEvent(LibraryAdministrationEvent.RetryReorderSynchronization) }) {
            Text(stringResource(R.string.library_reorder_retry_sync))
          }
        }
      }
    }

    val confirmationLibrary =
      uiState.libraries.firstOrNull { it.id == uiState.deleteConfirmationLibraryId }
    if (confirmationLibrary != null) {
      DeleteConfirmationDialog(
        title = stringResource(R.string.delete_library_title, confirmationLibrary.name),
        message = stringResource(R.string.delete_library_confirmation),
        onConfirm = { onEvent(LibraryAdministrationEvent.ConfirmDeleteLibrary) },
        onDismiss = { onEvent(LibraryAdministrationEvent.CancelDeleteLibrary) },
      )
    }
  }
}

@Composable
private fun libraryAdministrationErrorText(error: LibraryAdministrationError): String =
  when (error) {
    is LibraryAdministrationError.SafeMessage -> error.message
    LibraryAdministrationError.GenericScanStart ->
      stringResource(R.string.library_scan_start_failed)
    LibraryAdministrationError.GenericMatchStart ->
      stringResource(R.string.library_match_start_failed)
    LibraryAdministrationError.GenericSynchronization ->
      stringResource(R.string.library_scan_sync_failed)
    LibraryAdministrationError.GenericDelete -> stringResource(R.string.delete_library_failed)
    LibraryAdministrationError.GenericDeleteSynchronization ->
      stringResource(R.string.library_delete_sync_failed)
    LibraryAdministrationError.GenericReorderSynchronization ->
      stringResource(R.string.library_reorder_sync_failed)
  }

@ShelfDroidPreview
@Composable
private fun LibraryAdministrationContentPreview() {
  PreviewWrapper {
    LibraryAdministrationContent(
      uiState =
        LibraryAdministrationUiState(
          state = GenericState.Success,
          isRefreshing = false,
          libraries =
            listOf(
              LibraryAdministrationLibrary(
                id = "book-library",
                name = "Books",
                mediaType = LibraryAdministrationMediaType.BOOK,
                displayOrder = 0,
              ),
              LibraryAdministrationLibrary(
                id = "podcast-library",
                name = "Podcasts",
                mediaType = LibraryAdministrationMediaType.PODCAST,
                displayOrder = 1,
              ),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdministrationFailurePreview() {
  PreviewWrapper {
    LibraryAdministrationContent(
      uiState =
        LibraryAdministrationUiState(
          state = GenericState.Failure("The server could not be reached."),
          isRefreshing = false,
        )
    )
  }
}
