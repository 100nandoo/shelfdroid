package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.conflictingLibraryFolder
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun LibraryAdminFilesystemScreen(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
) {
  Surface(modifier = Modifier.fillMaxSize()) {
    LibraryAdminFilesystemContent(uiState, onEvent, Modifier.safeDrawingPadding())
  }
}

@Composable
internal fun LibraryAdminFilesystemContent(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  modifier: Modifier = Modifier,
) {
  val state = uiState.filesystemState
  val path =
    when (state) {
      is LibraryAdminFilesystemState.Success -> state.path
      is LibraryAdminFilesystemState.Loading -> state.path
      is LibraryAdminFilesystemState.Failure -> state.path
      LibraryAdminFilesystemState.Closed -> null
    }
  val conflict = path?.let { conflictingLibraryFolder(it, uiState.draft.folders) }
  Column(modifier.fillMaxSize()) {
    Row(
      Modifier.fillMaxWidth().padding(end = 16.dp, top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) }) {
        Icon(painterResource(R.drawable.close), stringResource(R.string.cancel))
      }
      Row(
        Modifier.weight(1f).horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        TextButton(onClick = { onEvent(LibraryAdminCreateEvent.OpenFilesystemPath("")) }) {
          Text(stringResource(R.string.library_filesystem_root))
        }
        uiState.filesystemHistory.forEach { ancestor ->
          Text("›")
          TextButton(onClick = { onEvent(LibraryAdminCreateEvent.OpenFilesystemPath(ancestor)) }) {
            Text(ancestor.trimEnd('/').substringAfterLast('/'))
          }
        }
      }
    }
    HorizontalDivider()
    Box(Modifier.weight(1f).fillMaxWidth()) {
      when (state) {
        is LibraryAdminFilesystemState.Loading -> LinearProgressIndicator(Modifier.fillMaxWidth())
        is LibraryAdminFilesystemState.Failure ->
          Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.library_filesystem_load_failed))
            TextButton(
              onClick = { onEvent(LibraryAdminCreateEvent.OpenFilesystemPath(path.orEmpty())) }
            ) {
              Text(stringResource(R.string.retry))
            }
          }
        is LibraryAdminFilesystemState.Success -> {
          if (state.filesystem.directories.isEmpty()) {
            Text(stringResource(R.string.library_filesystem_empty), Modifier.padding(16.dp))
          } else {
            val description = stringResource(R.string.library_filesystem_directory_list)
            key(path) {
              LazyColumn(
                modifier = Modifier.fillMaxSize().semantics { contentDescription = description },
                state = rememberLazyListState(),
              ) {
                items(state.filesystem.directories, key = { it.path }) { directory ->
                  LibraryAdminDirectoryItem(directory) {
                    onEvent(LibraryAdminCreateEvent.OpenFilesystemPath(directory.path))
                  }
                }
              }
            }
          }
        }
        LibraryAdminFilesystemState.Closed -> Unit
      }
    }
    HorizontalDivider()
    Column(
      Modifier.fillMaxWidth().padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      if (uiState.filesystemHistory.isNotEmpty()) {
        TextButton(onClick = { onEvent(LibraryAdminCreateEvent.FilesystemUp) }) {
          Icon(
            painter = painterResource(R.drawable.arrow_left),
            contentDescription = null,
          )
          Spacer(Modifier.width(8.dp))
          Text(stringResource(R.string.library_filesystem_up))
        }
      }
      Text(
        path ?: stringResource(R.string.library_filesystem_choose_folder),
        style = MaterialTheme.typography.bodyMedium,
      )
      if (conflict != null) {
        Text(
          stringResource(R.string.library_filesystem_conflict, conflict),
          color = MaterialTheme.colorScheme.error,
        )
      }
      Button(
        onClick = { path?.let { onEvent(LibraryAdminCreateEvent.SelectFolder(it)) } },
        enabled = state is LibraryAdminFilesystemState.Success && path != null && conflict == null,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.library_filesystem_add_current))
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdminFilesystemPreview() {
  PreviewWrapper {
    LibraryAdminFilesystemContent(
      LibraryAdminCreateUiState(filesystemState = LibraryAdminFilesystemState.Loading(null)),
      {},
    )
  }
}
