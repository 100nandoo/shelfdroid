package dev.halim.shelfdroid.core.ui.screen.apikeys.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
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
import dev.halim.shelfdroid.core.data.screen.apikeys.edit.EditApiKeysUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.EDIT_API_KEYS_UI_STATE_ACTIVE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun EditApiKeysScreen(
  viewModel: EditApiKeysViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  EditApiKeysContent(uiState = uiState, onEvent = viewModel::onEvent)

  SnackbarHandling(viewModel, snackbarHostState, navigateBack)
}

@Composable
private fun EditApiKeysContent(
  uiState: EditApiKeysUiState = EditApiKeysUiState(),
  onEvent: (EditApiKeysEvent) -> Unit = {},
) {
  Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    Spacer(modifier = Modifier.height(16.dp))

    TextTitleMedium(text = uiState.name, modifier = Modifier.padding(horizontal = 16.dp))

    val selectedUsername = uiState.users.find { it.id == uiState.selectedUserId }?.username ?: ""
    ChipDropdownMenu(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      label = stringResource(R.string.owner_semicolon),
      labelOnTop = false,
      options = uiState.users.map { it.username },
      initialValue = selectedUsername,
      onClick = { username ->
        val user = uiState.users.find { it.username == username }
        user?.let {
          onEvent(EditApiKeysEvent.Update { state -> state.copy(selectedUserId = user.id) })
        }
      },
    )

    MySwitch(
      modifier = Modifier.padding(horizontal = 16.dp),
      title = stringResource(R.string.enable),
      checked = uiState.isActive,
      contentDescription = stringResource(R.string.enable),
      onCheckedChange = {
        onEvent(EditApiKeysEvent.Update { it.copy(isActive = it.isActive.not()) })
      },
    )

    Spacer(modifier = Modifier.height(12.dp))

    Button(
      onClick = { onEvent(EditApiKeysEvent.Submit) },
      modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp),
    ) {
      Text(stringResource(R.string.submit))
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun SnackbarHandling(
  viewModel: EditApiKeysViewModel,
  snackbarHostState: SnackbarHostState,
  navigateBack: () -> Unit,
) {
  val successMessage = stringResource(R.string.api_key_updated)
  val errorMessage = stringResource(R.string.update_api_key_failed)

  LaunchedEffect(Unit) {
    viewModel.events.collect { event ->
      when (event) {
        is GenericUiEvent.ShowErrorSnackbar -> {
          val message = event.message.ifEmpty { errorMessage }
          launch { snackbarHostState.showErrorSnackbar(message) }
        }
        is GenericUiEvent.ShowSuccessSnackbar -> {
          launch { snackbarHostState.showSuccessSnackbar(successMessage) }
        }
        GenericUiEvent.NavigateBack -> navigateBack()
        else -> Unit
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EditApiKeysScreenContentPreview() {
  AnimatedPreviewWrapper { EditApiKeysContent(EDIT_API_KEYS_UI_STATE_ACTIVE) }
}
