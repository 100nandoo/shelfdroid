@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
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
import kotlinx.coroutines.isActive

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
  val listState = rememberLazyListState()
  val density = LocalDensity.current
  val edgeScrollZone = with(density) { 72.dp.toPx() }
  val maximumEdgeScrollSpeed = with(density) { 1_200.dp.toPx() }
  val draggedElevation = with(density) { 8.dp.toPx() }
  var displayedLibraries by remember { mutableStateOf(uiState.libraries) }
  var dragSession by remember { mutableStateOf<LibraryDragSession?>(null) }

  fun cancelDrag() {
    val session = dragSession ?: return
    displayedLibraries = session.sourceLibraries
    dragSession = null
  }

  fun moveDraggedLibrary(delta: Float) {
    val session = dragSession ?: return
    val currentInfo =
      listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == session.libraryId }
    val movedSession =
      session.copy(
        translationY = session.translationY + delta,
        pointerY = session.pointerY + delta,
      )
    if (currentInfo == null || currentInfo.index != session.currentIndex) {
      dragSession = movedSession
      return
    }

    val draggedCenter = currentInfo.offset + movedSession.translationY + currentInfo.size / 2f
    val targetInfo =
      listState.layoutInfo.visibleItemsInfo.dragTarget(
        draggedCenter = draggedCenter,
        currentIndex = session.currentIndex,
        libraries = displayedLibraries,
      )
    if (targetInfo == null) {
      dragSession = movedSession
      return
    }

    val targetIndex = displayedLibraries.indexOfFirst { it.id == targetInfo.key }
    if (targetIndex < 0 || targetIndex == session.currentIndex) {
      dragSession = movedSession
      return
    }
    val newBaseOffset =
      if (targetIndex > session.currentIndex) {
        targetInfo.offset + targetInfo.size - currentInfo.size
      } else {
        targetInfo.offset
      }
    displayedLibraries =
      displayedLibraries.toMutableList().apply {
        add(targetIndex, removeAt(session.currentIndex))
      }
    dragSession =
      movedSession.copy(
        currentIndex = targetIndex,
        translationY = movedSession.translationY + currentInfo.offset - newBaseOffset,
      )
  }

  LaunchedEffect(
    uiState.libraries,
    uiState.connectionState,
    uiState.taskStates,
    uiState.tasks,
    uiState.deletingLibraryId,
    uiState.deleteConfirmationLibraryId,
    uiState.isReordering,
  ) {
    dragSession = null
    displayedLibraries = uiState.libraries
  }

  LaunchedEffect(dragSession?.libraryId) {
    var previousFrameNanos = 0L
    while (isActive && dragSession != null) {
      val frameNanos = withFrameNanos { it }
      val session = dragSession ?: continue
      val layoutInfo = listState.layoutInfo
      val scrollSpeed =
        when {
          session.pointerY < layoutInfo.viewportStartOffset + edgeScrollZone ->
            -maximumEdgeScrollSpeed *
              ((layoutInfo.viewportStartOffset + edgeScrollZone - session.pointerY) /
                  edgeScrollZone)
                .coerceIn(0f, 1f)
          session.pointerY > layoutInfo.viewportEndOffset - edgeScrollZone ->
            maximumEdgeScrollSpeed *
              ((session.pointerY - (layoutInfo.viewportEndOffset - edgeScrollZone)) /
                  edgeScrollZone)
                .coerceIn(0f, 1f)
          else -> 0f
        }
      if (previousFrameNanos != 0L && scrollSpeed != 0f) {
        val elapsedSeconds = (frameNanos - previousFrameNanos) / 1_000_000_000f
        val consumed = listState.scrollBy(scrollSpeed * elapsedSeconds)
        dragSession = dragSession?.copy(translationY = session.translationY + consumed)
        moveDraggedLibrary(0f)
      }
      previousFrameNanos = frameNanos
    }
  }

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
        state = listState,
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
              items(displayedLibraries, key = { library -> library.id }) { library ->
                val session = dragSession
                val isDragging = session?.libraryId == library.id
                val reorderEnabled =
                  uiState.canReorder(library.id) &&
                    !uiState.isReordering &&
                    (session == null || isDragging)
                val itemModifier =
                  if (isDragging) {
                    Modifier.zIndex(1f).graphicsLayer {
                      translationY = session.translationY
                      scaleX = 1.02f
                      scaleY = 1.02f
                      shadowElevation = draggedElevation
                    }
                  } else {
                    Modifier.animateItem()
                  }
                LibraryAdminItem(
                  modifier = itemModifier,
                  library = library,
                  reorderEnabled = reorderEnabled,
                  reorderHandleVisible = reorderEnabled || session != null || uiState.isReordering,
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
                  onDragStart = {
                    val itemInfo =
                      listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == library.id }
                    val sourceIndex = displayedLibraries.indexOfFirst { it.id == library.id }
                    if (itemInfo != null && sourceIndex >= 0) {
                      dragSession =
                        LibraryDragSession(
                          libraryId = library.id,
                          sourceLibraries = displayedLibraries,
                          originalIndex = sourceIndex,
                          currentIndex = sourceIndex,
                          pointerY = itemInfo.offset + itemInfo.size / 2f,
                        )
                    }
                  },
                  onDrag = ::moveDraggedLibrary,
                  onDragEnd = {
                    val completedDrag = dragSession
                    dragSession = null
                    if (completedDrag != null) {
                      if (completedDrag.currentIndex == completedDrag.originalIndex) {
                        displayedLibraries = completedDrag.sourceLibraries
                      } else {
                        onEvent(
                          LibraryAdminEvent.MoveLibraryTo(
                            libraryId = completedDrag.libraryId,
                            destinationIndex = completedDrag.currentIndex,
                          )
                        )
                      }
                    }
                  },
                  onDragCancel = ::cancelDrag,
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

private data class LibraryDragSession(
  val libraryId: String,
  val sourceLibraries: List<LibraryAdminLibrary>,
  val originalIndex: Int,
  val currentIndex: Int,
  val translationY: Float = 0f,
  val pointerY: Float,
)

private fun List<LazyListItemInfo>.dragTarget(
  draggedCenter: Float,
  currentIndex: Int,
  libraries: List<LibraryAdminLibrary>,
): LazyListItemInfo? {
  val currentInfo = firstOrNull { it.index == currentIndex } ?: return null
  val currentCenter = currentInfo.offset + currentInfo.size / 2f
  return if (draggedCenter > currentCenter) {
    filter { item ->
      val index = libraries.indexOfFirst { it.id == item.key }
      index > currentIndex && draggedCenter > item.offset + item.size / 2f
    }
      .maxByOrNull { item -> libraries.indexOfFirst { it.id == item.key } }
  } else {
    filter { item ->
      val index = libraries.indexOfFirst { it.id == item.key }
      index in 0 until currentIndex && draggedCenter < item.offset + item.size / 2f
    }
      .minByOrNull { item -> libraries.indexOfFirst { it.id == item.key } }
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
