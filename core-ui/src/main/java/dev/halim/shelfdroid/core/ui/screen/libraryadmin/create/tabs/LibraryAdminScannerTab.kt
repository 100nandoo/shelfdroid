@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ReorderHandle
import dev.halim.shelfdroid.core.ui.components.rememberReorderableLazyListState
import dev.halim.shelfdroid.core.ui.components.reorderableItemModifier
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateEvent
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.createErrorText

@Composable
internal fun LibraryAdminScannerTab(
  title: String,
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
  listState: LazyListState,
  modifier: Modifier = Modifier,
) {
  val reorderState =
    rememberReorderableLazyListState(
      items = uiState.draft.metadataSources,
      key = { source -> source.id },
      lazyListState = listState,
      enabled = !uiState.isBusy,
      onMove = { sourceId, destinationIndex ->
        val sourceIndex = uiState.draft.metadataSources.indexOfFirst { it.id == sourceId }
        if (sourceIndex >= 0) {
          onEvent(
            LibraryAdminCreateEvent.MoveMetadataSource(
              id = sourceId,
              delta = destinationIndex - sourceIndex,
            )
          )
        }
      },
    )
  val displayedDraft = uiState.draft.copy(metadataSources = reorderState.items)

  LazyColumn(
    state = listState,
    modifier =
      modifier
        .fillMaxWidth()
        .focusRequester(focusRequester)
        .focusable()
        .testTag(LIBRARY_ADMIN_SCANNER_LIST_TAG),
    contentPadding = PaddingValues(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item(key = "scanner-header") {
      androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, modifier = Modifier.padding(vertical = 12.dp))
        Text(stringResource(R.string.library_scanner_heading))
        Text(stringResource(R.string.library_scanner_description))
      }
    }
    items(reorderState.items, key = { source -> source.id }) { source ->
      val index = reorderState.indexOf(source.id)
      val priority = displayedDraft.metadataPriority(source.id)
      val sourceDescription =
        if (priority == null) source.name
        else stringResource(R.string.library_scanner_source_priority, source.name, priority)
      LibraryAdminScannerItem(
        modifier = reorderableItemModifier(reorderState, source.id),
        source = source,
        priority = priority,
        sourceDescription = sourceDescription,
        onCheckedChange = {
          onEvent(LibraryAdminCreateEvent.ToggleMetadataSource(source.id, it))
        },
        reorderHandle = {
          ReorderHandle(
            state = reorderState,
            itemKey = source.id,
            enabled = !uiState.isBusy,
            contentDescription =
              stringResource(
                R.string.reorder_item_position,
                source.name,
                index + 1,
                reorderState.items.size,
              ),
            moveUpLabel = stringResource(R.string.move_item_up, source.name),
            moveDownLabel = stringResource(R.string.move_item_down, source.name),
          )
        },
      )
    }
    if (uiState.validation.errors.containsKey(LibraryAdminCreateField.SCANNER_PRECEDENCE)) {
      item(key = "scanner-error") {
        val scannerErrorDescription = stringResource(R.string.library_scanner_validation)
        Text(
          text =
            createErrorText(
              uiState.validation.errors.getValue(LibraryAdminCreateField.SCANNER_PRECEDENCE)
            ),
          modifier = Modifier.semantics { contentDescription = scannerErrorDescription },
        )
      }
    }
  }
}

internal const val LIBRARY_ADMIN_SCANNER_LIST_TAG = "library-admin-scanner-list"
