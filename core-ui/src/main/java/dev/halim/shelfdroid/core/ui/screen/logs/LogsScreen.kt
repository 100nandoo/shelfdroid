package dev.halim.shelfdroid.core.ui.screen.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiState
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LOG_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel(), snackbarHostState: SnackbarHostState) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  LogsContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun LogsContent(uiState: LogsUiState = LogsUiState(), onEvent: (LogsEvent) -> Unit = {}) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    LazyColumn(Modifier.fillMaxSize(), reverseLayout = false) {
      items(uiState.logs, key = { it.id }) { logItem ->
        when (logItem) {
          is LogsUiState.LogItem.HourHeader -> HourHeader(logItem)
          is LogsUiState.LogItem.Log -> LogItem(logItem)
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun LogsScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { LogsContent(LOG_UI_STATE) }
}
