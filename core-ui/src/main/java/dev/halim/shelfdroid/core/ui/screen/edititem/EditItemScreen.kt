package dev.halim.shelfdroid.core.ui.screen.edititem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemTab
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChaptersTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.CoverTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.DetailsTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.FilesTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.MatchTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ToolsTab

@Composable
fun EditItemScreen(
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
  viewModel: EditItemViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val savedMessage = stringResource(R.string.edit_item_saved)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is GenericUiEvent.ShowSuccessSnackbar ->
          snackbarHostState.showSuccessSnackbar(event.message.ifBlank { savedMessage })
        is GenericUiEvent.ShowErrorSnackbar -> snackbarHostState.showErrorSnackbar(event.message)
        is GenericUiEvent.ShowPlainSnackbar -> snackbarHostState.showSnackbar(event.message)
        GenericUiEvent.NavigateBack -> navigateBack()
      }
    }
  }

  when (val state = uiState.state) {
    GenericState.Loading,
    GenericState.Idle -> {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
      }
    }
    is GenericState.Failure -> {
      Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(state.errorMessage ?: stringResource(R.string.edit_item_failed_to_load))
      }
    }
    GenericState.Success -> {
      EditItemContent(uiState = uiState, onEvent = viewModel::onEvent)
    }
  }
}

@Composable
private fun EditItemContent(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.isSaving) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Box(
      modifier =
        Modifier.weight(1f)
          .fillMaxWidth()
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(16.dp)
    ) {
      when (uiState.currentTab) {
        EditItemTab.Details -> DetailsTab(uiState.details, onEvent, uiState.seriesSuggestions)
        EditItemTab.Cover -> CoverTab(uiState, onEvent)
        EditItemTab.Chapters -> ChaptersTab(uiState)
        EditItemTab.Files -> FilesTab(uiState)
        EditItemTab.Match -> MatchTab(uiState, onEvent)
        EditItemTab.Tools -> ToolsTab(uiState, onEvent)
      }
    }

    val tabs = EditItemTab.entries
    val selectedIndex = tabs.indexOf(uiState.currentTab)

    SecondaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
      tabs.forEach { tab ->
        Tab(
          selected = uiState.currentTab == tab,
          onClick = { onEvent(EditItemEvent.ChangeTab(tab)) },
          text = { Text(tab.name) },
        )
      }
    }
  }
}
