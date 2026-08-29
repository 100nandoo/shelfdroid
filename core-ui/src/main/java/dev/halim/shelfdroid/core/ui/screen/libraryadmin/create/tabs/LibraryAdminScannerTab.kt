@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateEvent
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.createErrorText

@Composable
internal fun LibraryAdminScannerTab(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .focusRequester(focusRequester)
        .focusable(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_scanner_heading))
    Text(stringResource(R.string.library_scanner_description))
    uiState.draft.metadataSources.forEachIndexed { index, source ->
      val priority = uiState.draft.metadataPriority(source.id)
      val sourceDescription =
        if (priority == null) source.name
        else stringResource(R.string.library_scanner_source_priority, source.name, priority)
      Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = sourceDescription },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(priority?.toString().orEmpty(), modifier = Modifier.width(28.dp))
        MySwitch(
          modifier = Modifier.weight(1f),
          title = source.name,
          checked = source.enabled,
          contentDescription = source.name,
          onCheckedChange = {
            onEvent(LibraryAdminCreateEvent.ToggleMetadataSource(source.id, it))
          },
        )
        TextButton(
          enabled = index > 0,
          onClick = { onEvent(LibraryAdminCreateEvent.MoveMetadataSource(source.id, -1)) },
        ) {
          Text(stringResource(R.string.move_up))
        }
        TextButton(
          enabled = index < uiState.draft.metadataSources.lastIndex,
          onClick = { onEvent(LibraryAdminCreateEvent.MoveMetadataSource(source.id, 1)) },
        ) {
          Text(stringResource(R.string.move_down))
        }
      }
    }
    if (uiState.validation.errors.containsKey(LibraryAdminCreateField.SCANNER_PRECEDENCE)) {
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
