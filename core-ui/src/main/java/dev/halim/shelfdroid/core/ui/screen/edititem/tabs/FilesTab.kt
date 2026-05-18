package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyFilledTonalIconButton
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun FilesTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  MyAlertDialog(
    showDialog = uiState.pendingDeleteFile != null,
    title = stringResource(R.string.delete),
    text = stringResource(R.string.edit_item_delete_file_confirm),
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = { onEvent(EditItemEvent.ConfirmDeleteLibraryFile) },
    onDismiss = { onEvent(EditItemEvent.DismissDeleteLibraryFile) },
  )

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(R.string.edit_item_library_files_count, uiState.libraryFiles.size),
      style = MaterialTheme.typography.titleMedium,
    )
    HorizontalDivider()
    uiState.libraryFiles.forEach { file ->
      val isBusy = uiState.activeFileActionIno == file.ino
      Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = file.filename, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(modifier = Modifier.fillMaxWidth()) {
          Text(
            text = "${file.sizeText} ∙ ${file.fileType}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          MyFilledTonalIconButton(
            enabled = !isBusy,
            onClick = { onEvent(EditItemEvent.DownloadLibraryFile(file.ino)) },
            painter = painterResource(R.drawable.download),
            contentDescription = stringResource(R.string.download),
          )
          MyFilledTonalIconButton(
            enabled = !isBusy,
            onClick = { onEvent(EditItemEvent.PromptDeleteLibraryFile(file)) },
            painter = painterResource(R.drawable.delete),
            contentDescription = stringResource(R.string.delete),
          )
        }
        HorizontalDivider()
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun FilesTabPreview() {
  PreviewWrapper { FilesTab(uiState = Defaults.EDIT_ITEM_UI_STATE, onEvent = {}) }
}
