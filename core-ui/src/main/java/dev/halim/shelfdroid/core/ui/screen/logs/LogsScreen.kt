package dev.halim.shelfdroid.core.ui.screen.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.extensions.capitalized
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

    LazyColumn(Modifier.fillMaxSize(), reverseLayout = true) {
      item(key = "control") { Control(Modifier.animateItem(), uiState, onEvent) }

      items(filterLogs(uiState), key = { it.id }) { logItem ->
        when (logItem) {
          is LogsUiState.LogItem.HourHeader -> HourHeader(logItem)
          is LogsUiState.LogItem.Log -> LogItem(logItem)
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

private fun filterLogs(uiState: LogsUiState): List<LogsUiState.LogItem> {
  val filtered = mutableListOf<LogsUiState.LogItem>()
  val pendingLogs = mutableListOf<LogsUiState.LogItem.Log>()

  for (item in uiState.logs) {
    when (item) {
      is LogsUiState.LogItem.Log -> {
        if (item.level >= uiState.filterLogLevel) {
          pendingLogs.add(item)
        }
      }
      is LogsUiState.LogItem.HourHeader -> {
        if (pendingLogs.isNotEmpty()) {
          filtered.addAll(pendingLogs)
          filtered.add(item)
          pendingLogs.clear()
        }
      }
    }
  }
  filtered.addAll(pendingLogs)
  return filtered
}

@Composable
private fun Control(
  modifier: Modifier = Modifier,
  uiState: LogsUiState,
  onEvent: (LogsEvent) -> Unit = {},
) {
  val options = remember {
    LogLevel.entries.filter { it != LogLevel.ERROR }.map { it.name.capitalized() }
  }

  Column(modifier = modifier.padding(16.dp)) {
    Row(modifier = Modifier.fillMaxWidth()) {
      ChipDropdownMenu(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.server_log_level),
        labelOnTop = true,
        options = options,
        initialValue = uiState.logLevel.name.capitalized(),
        onClick = { selected ->
          val logLevel = LogLevel.valueOf(selected.uppercase())
          onEvent(LogsEvent.ChangeLogLevel(logLevel))
        },
      )
      ChipDropdownMenu(
        modifier = Modifier.weight(1f),
        label = stringResource(R.string.filter_log_level),
        options = options,
        labelOnTop = true,
        initialValue = uiState.filterLogLevel.name.capitalized(),
        onClick = { selected ->
          val logLevel = LogLevel.valueOf(selected.uppercase())
          onEvent(LogsEvent.UpdateUiState { it.copy(filterLogLevel = logLevel) })
        },
      )
    }
  }
  HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
}

@ShelfDroidPreview
@Composable
fun LogsScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { LogsContent(LOG_UI_STATE) }
}

@ShelfDroidPreview
@Composable
fun LogsScreenControlPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { Control(uiState = LOG_UI_STATE) {} }
}
