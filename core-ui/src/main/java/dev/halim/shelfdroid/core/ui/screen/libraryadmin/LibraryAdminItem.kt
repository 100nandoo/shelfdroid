@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material3.ListItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMediaType
import dev.halim.shelfdroid.core.data.task.ServerTaskError
import dev.halim.shelfdroid.core.data.task.ServerTask
import dev.halim.shelfdroid.core.data.task.ServerTaskSyncState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

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
  task: ServerTask? = null,
  onRetrySynchronization: (taskId: String) -> Unit = {},
) {
  var dragDistance = 0f
  val dragHandleDescription = stringResource(R.string.reorder_library)
  val scanDescription = stringResource(R.string.scan_library)
  val matchDescription = stringResource(R.string.match_book_metadata)
  val deleteDescription = stringResource(R.string.delete_library)
  val dragModifier =
    if (reorderEnabled) {
      Modifier.pointerInput(library.id) {
        detectDragGesturesAfterLongPress(
          onDragStart = { dragDistance = 0f },
          onDragCancel = { dragDistance = 0f },
          onDragEnd = { dragDistance = 0f },
        ) { change, dragAmount ->
          change.consume()
          dragDistance += dragAmount.y
          // A row-height threshold makes drag reorder deterministic and keeps small pointer
          // movement from generating a stream of server requests.
          if (dragDistance <= -48f) {
            onDragMove(-1)
            dragDistance = 0f
          } else if (dragDistance >= 48f) {
            onDragMove(1)
            dragDistance = 0f
          }
        }
      }
    } else Modifier

  ListItem(
    modifier = dragModifier.fillMaxWidth(),
    headlineContent = { Text(library.name) },
    supportingContent = {
      Column {
        Text(libraryTypeText(library.mediaType))
        Text(stringResource(R.string.library_identity, library.id))
        if (task != null) {
          val presentation = serverTaskPresentation(task.action, task.status)
          Text(stringResource(presentation.statusLabel))
          task.result?.let { result ->
            when (presentation.kind) {
              ServerTaskPresentationKind.LIBRARY_SCAN ->
                Text(
                  stringResource(
                    presentation.countsLabel ?: R.string.library_scan_counts,
                    result.added ?: 0,
                    result.updated ?: 0,
                    result.missing ?: 0,
                  )
                )
              ServerTaskPresentationKind.BOOK_MATCHING ->
                Text(
                  stringResource(
                    presentation.countsLabel ?: R.string.library_match_counts,
                    result.updated ?: 0,
                  )
                )
              ServerTaskPresentationKind.UNKNOWN -> Unit
            }
          }
          task.result?.elapsedMillis?.let { elapsed ->
            presentation.elapsedLabel?.let { elapsedLabel ->
              Text(stringResource(elapsedLabel, elapsed / 1000))
            }
          }
          if (task.syncState == ServerTaskSyncState.FAILED) {
            Text(
              text = stringResource(R.string.library_task_sync_failed),
              color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
            TextButton(onClick = { onRetrySynchronization(task.id) }) {
              Text(stringResource(R.string.library_scan_retry_sync))
            }
          }
          task.error?.let { error -> Text(serverTaskErrorText(error)) }
        }
      }
    },
    trailingContent = {
      Row(modifier = Modifier.padding(start = 8.dp)) {
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
        if (library.mediaType == LibraryAdminMediaType.BOOK) {
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
          Icon(
            painter = painterResource(R.drawable.drag_handle),
            contentDescription = dragHandleDescription,
          )
        }
      }
    },
  )
}

@Composable
private fun serverTaskErrorText(error: ServerTaskError): String =
  when (error) {
    is ServerTaskError.SafeMessage -> error.message
    ServerTaskError.Generic -> stringResource(R.string.library_task_failed_generic)
  }

@Composable
private fun libraryTypeText(mediaType: LibraryAdminMediaType): String =
  when (mediaType) {
    LibraryAdminMediaType.BOOK -> stringResource(R.string.book_library)
    LibraryAdminMediaType.PODCAST -> stringResource(R.string.podcast_library)
    LibraryAdminMediaType.UNKNOWN -> stringResource(R.string.library_type_unknown)
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
          mediaType = LibraryAdminMediaType.BOOK,
          displayOrder = 0,
        )
    )
  }
}
