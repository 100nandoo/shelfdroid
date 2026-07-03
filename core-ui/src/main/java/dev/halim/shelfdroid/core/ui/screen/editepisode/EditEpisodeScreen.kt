package dev.halim.shelfdroid.core.ui.screen.editepisode

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import dev.halim.shelfdroid.core.data.screen.editepisode.EditEpisodeUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.navigation.EditEpisode
import dev.halim.shelfdroid.core.ui.screen.editepisode.tabs.EditEpisodeDetailsTab

@Composable
fun EditEpisodeScreen(
  navKey: EditEpisode,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
  viewModel: EditEpisodeViewModel =
    hiltViewModel<EditEpisodeViewModel, EditEpisodeViewModel.Factory> { factory ->
      factory.create(navKey)
    },
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
        is GenericUiEvent.RequestManagedDownload -> Unit
      }
    }
  }

  EditEpisodeScreenStateContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun EditEpisodeScreenStateContent(
  uiState: EditEpisodeUiState,
  onEvent: (EditEpisodeEvent) -> Unit,
) {
  Column(modifier = Modifier.fillMaxSize()) {
    if (uiState.isSaving) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Box(modifier = Modifier.weight(1f).fillMaxWidth().imePadding()) {
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
          EditEpisodeDetailsTab(
            podcastTitle = uiState.podcastTitle,
            details = uiState.details,
            canSave = uiState.canSave(),
            onEvent = onEvent,
            onSave = { onEvent(EditEpisodeEvent.Save) },
          )
        }
      }
    }

    if (uiState.state == GenericState.Success) {
      SecondaryScrollableTabRow(selectedTabIndex = 0, edgePadding = 0.dp) {
        Tab(selected = true, onClick = {}, text = { Text(stringResource(R.string.details)) })
      }
    }
  }
}
