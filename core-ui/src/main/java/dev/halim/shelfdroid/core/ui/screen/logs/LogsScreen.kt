package dev.halim.shelfdroid.core.ui.screen.logs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
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
fun LogsScreen(viewModel: LogsViewModel = hiltViewModel()) {
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
          is LogsUiState.LogItem.HourHeader -> HourHeader(logItem, Modifier.animateItem())
          is LogsUiState.LogItem.Log -> LogItem(logItem, Modifier.animateItem())
        }
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

private fun filterLogs(uiState: LogsUiState): List<LogsUiState.LogItem> {
  val filtered = mutableListOf<LogsUiState.LogItem>()
  val pendingLogs = mutableListOf<LogsUiState.LogItem.Log>()
  val query = uiState.searchQuery.trim()

  for (item in uiState.logs) {
    when (item) {
      is LogsUiState.LogItem.Log -> {
        val matchesLevel = item.level >= uiState.filterLogLevel
        val matchesQuery = query.isBlank() || item.message.contains(query, ignoreCase = true)

        if (matchesLevel && matchesQuery) {
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
    Spacer(modifier = Modifier.height(16.dp))
    SearchBar(uiState, onEvent)
  }
  HorizontalDivider(modifier = Modifier.padding(top = 16.dp))
}

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
private fun SearchBar(uiState: LogsUiState, onEvent: (LogsEvent) -> Unit) {
  val logSuggestions = remember(uiState.logs) { extractLogSuggestions(uiState.logs) }
  val suggestionQuery = uiState.searchQuery.trim()
  val filteredSuggestions =
    remember(logSuggestions, suggestionQuery) {
      logSuggestions.filter {
        suggestionQuery.isBlank() || it.contains(suggestionQuery, ignoreCase = true)
      }
    }
  val isKeyboardVisible = WindowInsets.isImeVisible
  val showSuggestions = isKeyboardVisible && filteredSuggestions.isNotEmpty()

  var textFieldValue by
    remember(suggestionQuery) {
      mutableStateOf(TextFieldValue(suggestionQuery, TextRange(suggestionQuery.length)))
    }

  ExposedDropdownMenuBox(
    expanded = showSuggestions,
    onExpandedChange = {},
    modifier = Modifier.fillMaxWidth().imePadding(),
  ) {
    OutlinedTextField(
      value = textFieldValue,
      onValueChange = {
        onEvent(LogsEvent.UpdateUiState { state -> state.copy(searchQuery = it.text) })
      },
      modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
      placeholder = { Text(stringResource(R.string.search_logs)) },
      leadingIcon = {
        Icon(
          painter = painterResource(id = R.drawable.search),
          contentDescription = stringResource(R.string.search_logs),
        )
      },
      trailingIcon = {
        AnimatedVisibility(uiState.searchQuery.isNotEmpty()) {
          IconButton(
            onClick = { onEvent(LogsEvent.UpdateUiState { state -> state.copy(searchQuery = "") }) }
          ) {
            Icon(
              painter = painterResource(id = R.drawable.close),
              contentDescription = stringResource(R.string.clear_log_search),
            )
          }
        }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = {}),
    )
    ExposedDropdownMenu(expanded = showSuggestions, onDismissRequest = {}) {
      filteredSuggestions.forEach { suggestion ->
        DropdownMenuItem(
          text = { Text(suggestion) },
          onClick = {
            onEvent(LogsEvent.UpdateUiState { state -> state.copy(searchQuery = suggestion) })
          },
          contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
        )
      }
    }
  }
}

private fun extractLogSuggestions(logs: List<LogsUiState.LogItem>): List<String> {
  val regex = "^\\s*\\[([^\\]]+)]".toRegex()

  return logs
    .asSequence()
    .filterIsInstance<LogsUiState.LogItem.Log>()
    .mapNotNull { log -> regex.find(log.message)?.groupValues?.getOrNull(1)?.trim() }
    .filter { it.isNotBlank() }
    .distinct()
    .sorted()
    .toList()
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
