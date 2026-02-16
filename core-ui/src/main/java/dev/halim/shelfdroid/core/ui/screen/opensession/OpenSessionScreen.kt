@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.opensession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session
import dev.halim.shelfdroid.core.data.screen.opensession.OpenSessionApiState
import dev.halim.shelfdroid.core.data.screen.opensession.OpenSessionUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ListItemAction
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSIONS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import dev.halim.shelfdroid.core.ui.screen.listeningsession.ListeningSessionItem
import dev.halim.shelfdroid.core.ui.screen.listeningsession.ListeningSessionSheet
import kotlinx.coroutines.launch

@Composable
fun OpenSessionScreen(
  viewModel: OpenSessionViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  OpenSessionContent(uiState = uiState, onEvent = viewModel::onEvent)

  val scope = rememberCoroutineScope()
  val closeFailureMessage =
    stringResource(R.string.args_not_found, pluralStringResource(R.plurals.plurals_open_session, 1))

  LaunchedEffect(uiState.apiState) {
    when (uiState.apiState) {
      is OpenSessionApiState.CloseFailure -> {
        scope.launch { snackbarHostState.showSnackbar(closeFailureMessage) }
      }
      else -> Unit
    }
  }
}

@Composable
private fun OpenSessionContent(
  uiState: OpenSessionUiState = OpenSessionUiState(),
  onEvent: (OpenSessionEvent) -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedSession by remember { mutableStateOf(Session("")) }

  ListeningSessionSheet(
    sheetState,
    selectedSession,
    {
      ListItemAction(
        text = stringResource(R.string.close_open_session),
        contentDescription = stringResource(R.string.close_open_session),
        icon = R.drawable.cancel,
        onClick = {
          scope.launch { sheetState.hide() }
          onEvent(OpenSessionEvent.CloseSession(selectedSession.id))
        },
      )
    },
  )

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    AnimatedVisibility(uiState.sessions.isEmpty()) {
      GenericMessageScreen(
        stringResource(R.string.empty_type, stringResource(R.string.open_sessions))
      )
    }

    AnimatedVisibility(uiState.sessions.isNotEmpty()) {
      LazyColumn(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = true,
      ) {
        items(uiState.sessions, key = { it.id }) { session ->
          HorizontalDivider()

          ListeningSessionItem(
            session,
            enableSelection = false,
            isSelected = false,
            isSelectionMode = false,
            showSheet = {
              selectedSession = session
              scope.launch { sheetState.show() }
            },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun OpenSessionScreenContentPreview() {
  val uiState = OpenSessionUiState(state = GenericState.Success, sessions = LISTENING_SESSIONS)
  AnimatedPreviewWrapper(dynamicColor = false) { OpenSessionContent(uiState = uiState) }
}
