package dev.halim.shelfdroid.core.ui.screen.edititem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.download.ManagedDownload
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemTab
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.coerceFor
import dev.halim.shelfdroid.core.data.screen.edititem.supportedTabs
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.navigation.EditItem
import dev.halim.shelfdroid.core.ui.permissions.rememberNotificationPermissionHandler
import dev.halim.shelfdroid.core.ui.preview.Defaults.EDIT_ITEM_PODCAST_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.Defaults.EDIT_ITEM_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChaptersTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.CoverTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.DetailsTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.FilesTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.MatchTab
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ToolsTab

@Composable
fun EditItemScreen(
  navKey: EditItem,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
  viewModel: EditItemViewModel =
    hiltViewModel<EditItemViewModel, EditItemViewModel.Factory> { factory ->
      factory.create(navKey)
    },
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val savedMessage = stringResource(R.string.edit_item_saved)
  var pendingManagedDownload by remember { mutableStateOf<ManagedDownload?>(null) }
  val requestNotificationPermission =
    rememberNotificationPermissionHandler(
      snackbarHostState = snackbarHostState,
      onPermissionGranted = {
        pendingManagedDownload?.let(viewModel::enqueueManagedDownload)
        pendingManagedDownload = null
      },
    )

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is GenericUiEvent.ShowSuccessSnackbar ->
          snackbarHostState.showSuccessSnackbar(event.message.ifBlank { savedMessage })

        is GenericUiEvent.ShowErrorSnackbar -> snackbarHostState.showErrorSnackbar(event.message)
        is GenericUiEvent.ShowPlainSnackbar -> snackbarHostState.showSnackbar(event.message)
        is GenericUiEvent.RequestManagedDownload -> {
          pendingManagedDownload = event.download
          requestNotificationPermission()
        }
        GenericUiEvent.NavigateBack -> navigateBack()
      }
    }
  }

  EditItemScreenStateContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun EditItemScreenStateContent(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
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
      EditItemContent(uiState = uiState, onEvent = onEvent)
    }
  }
}

@Composable
private fun EditItemContent(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val currentTab = uiState.currentTab.coerceFor(uiState.mediaKind)
  val tabs = uiState.supportedTabs()

  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.isSaving) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    Box(modifier = Modifier.weight(1f).fillMaxWidth().imePadding()) {
      when (currentTab) {
        EditItemTab.Details ->
          DetailsTab(
            mediaKind = uiState.mediaKind,
            details = uiState.details,
            onEvent = onEvent,
            seriesSuggestions = uiState.seriesSuggestions,
          )
        EditItemTab.Cover -> CoverTab(uiState, onEvent)
        EditItemTab.Chapters -> ChaptersTab(uiState)
        EditItemTab.Files -> FilesTab(uiState, onEvent)
        EditItemTab.Match -> MatchTab(uiState, onEvent)
        EditItemTab.Tools -> ToolsTab(uiState, onEvent)
      }
    }

    val selectedIndex = tabs.indexOf(currentTab)

    SecondaryScrollableTabRow(selectedTabIndex = selectedIndex, edgePadding = 0.dp) {
      tabs.forEach { tab ->
        Tab(
          selected = currentTab == tab,
          onClick = { onEvent(EditItemEvent.ChangeTab(tab)) },
          text = { Text(tab.name) },
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun EditItemScreenContentPreview() {
  PreviewWrapper { EditItemScreenStateContent(uiState = EDIT_ITEM_UI_STATE, onEvent = {}) }
}

@ShelfDroidPreview
@Composable
private fun EditItemScreenSavingPreview() {
  PreviewWrapper {
    EditItemScreenStateContent(
      uiState = EDIT_ITEM_UI_STATE.copy(isSaving = true, currentTab = EditItemTab.Tools),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun EditItemScreenPodcastPreview() {
  PreviewWrapper { EditItemScreenStateContent(uiState = EDIT_ITEM_PODCAST_UI_STATE, onEvent = {}) }
}

@ShelfDroidPreview
@Composable
private fun EditItemScreenLoadingPreview() {
  PreviewWrapper {
    EditItemScreenStateContent(
      uiState = EDIT_ITEM_UI_STATE.copy(state = GenericState.Loading),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun EditItemScreenFailurePreview() {
  PreviewWrapper {
    EditItemScreenStateContent(
      uiState = EDIT_ITEM_UI_STATE.copy(state = GenericState.Failure("Failed to load item")),
      onEvent = {},
    )
  }
}
