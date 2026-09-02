@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.task.Task
import dev.halim.shelfdroid.core.data.task.TaskError
import dev.halim.shelfdroid.core.data.task.TaskSyncState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.libraryIconResource

@Composable
fun LibraryAdminItem(
  library: LibraryAdminLibrary,
  onDragMove: (delta: Int) -> Unit = {},
  reorderEnabled: Boolean = false,
  scanEnabled: Boolean = false,
  onScan: () -> Unit = {},
  matchEnabled: Boolean = false,
  onMatch: () -> Unit = {},
  deleteEnabled: Boolean = false,
  onDelete: () -> Unit = {},
  onEdit: () -> Unit = {},
  task: Task? = null,
  onRetrySynchronization: (taskId: String) -> Unit = {},
) {
  val dragDistance = remember { mutableFloatStateOf(0f) }
  val dragHandleDescription = stringResource(R.string.reorder_library)
  val scanDescription = stringResource(R.string.scan_library)
  val matchDescription = stringResource(R.string.match_book_metadata)
  val deleteDescription = stringResource(R.string.delete_library)
  val dragState = rememberDraggableState { delta ->
    dragDistance.floatValue += delta
    // A row-height threshold makes drag reorder deterministic and keeps small pointer movement
    // from generating a stream of server requests.
    if (dragDistance.floatValue <= -48f) {
      onDragMove(-1)
      dragDistance.floatValue = 0f
    } else if (dragDistance.floatValue >= 48f) {
      onDragMove(1)
      dragDistance.floatValue = 0f
    }
  }
  val dragModifier =
    if (reorderEnabled) {
      Modifier.draggable(
        state = dragState,
        orientation = Orientation.Vertical,
        startDragImmediately = true,
        onDragStarted = {
          dragDistance.floatValue = 0f
        },
        onDragStopped = { dragDistance.floatValue = 0f },
      )
    } else Modifier

  ListItem(
    modifier = Modifier.fillMaxWidth().clickable(onClick = onEdit),
    headlineContent = { Text(library.name) },
    leadingContent = {
      Icon(
        painter = painterResource(libraryIconResource(library.icon)),
        contentDescription = null,
      )
    },
    supportingContent = {
      Column {
        if (task != null) {
          val presentation = taskPresentation(task.action, task.status)
          Text(stringResource(presentation.statusLabel))
          task.result?.let { result ->
            when (presentation.kind) {
              TaskPresentationKind.LIBRARY_SCAN ->
                Text(
                  stringResource(
                    presentation.countsLabel ?: R.string.library_scan_counts,
                    result.added ?: 0,
                    result.updated ?: 0,
                    result.missing ?: 0,
                  )
                )
              TaskPresentationKind.BOOK_MATCHING ->
                Text(
                  stringResource(
                    presentation.countsLabel ?: R.string.library_match_counts,
                    result.updated ?: 0,
                  )
                )
              TaskPresentationKind.UNKNOWN -> Unit
            }
          }
          task.result?.elapsedMillis?.let { elapsed ->
            presentation.elapsedLabel?.let { elapsedLabel ->
              Text(stringResource(elapsedLabel, elapsed / 1000))
            }
          }
          if (task.syncState == TaskSyncState.FAILED) {
            Text(
              text = stringResource(R.string.library_task_sync_failed),
              color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = { onRetrySynchronization(task.id) }) {
              Text(stringResource(R.string.library_scan_retry_sync))
            }
          }
          task.error?.let { error -> Text(taskErrorText(error)) }
        }
      }
    },
    trailingContent = {
      Row(
        modifier = Modifier.padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TooltipBox(
          positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
          state = rememberTooltipState(),
          tooltip = { PlainTooltip { Text(scanDescription) } },
        ) {
          FilledTonalIconButton(enabled = scanEnabled, onClick = onScan) {
            Icon(
              painter = painterResource(R.drawable.refresh),
              contentDescription = scanDescription,
            )
          }
        }
        if (library.mediaType == MediaType.BOOK) {
          TooltipBox(
            positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
            state = rememberTooltipState(),
            tooltip = { PlainTooltip { Text(matchDescription) } },
          ) {
            FilledTonalIconButton(enabled = matchEnabled, onClick = onMatch) {
              Icon(
                painter = painterResource(R.drawable.wand_shine),
                contentDescription = matchDescription,
              )
            }
          }
        }
        TooltipBox(
          positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
          state = rememberTooltipState(),
          tooltip = { PlainTooltip { Text(deleteDescription) } },
        ) {
          FilledTonalIconButton(enabled = deleteEnabled, onClick = onDelete) {
            Icon(
              painter = painterResource(R.drawable.delete),
              contentDescription = deleteDescription,
            )
          }
        }
        if (reorderEnabled) {
          Box(
            modifier =
              Modifier.size(48.dp).then(dragModifier).semantics(mergeDescendants = true) {
                contentDescription = dragHandleDescription
              },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              painter = painterResource(R.drawable.drag_handle),
              contentDescription = null,
            )
          }
        }
      }
    },
  )
}

@Composable
private fun taskErrorText(error: TaskError): String =
  when (error) {
    is TaskError.SafeMessage -> error.message
    TaskError.Generic -> stringResource(R.string.library_task_failed_generic)
  }

@ShelfDroidPreview
@Composable
private fun LibraryAdminItemPreview() {
  PreviewWrapper {
    LibraryAdminItem(
      library =
        LibraryAdminLibrary(
          id = "books",
          name = "Books",
          mediaType = MediaType.BOOK,
          displayOrder = 0,
        ),
      reorderEnabled = true,
    )
  }
}
