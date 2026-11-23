package dev.halim.shelfdroid.core.ui.screen.settings.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.navigation.SettingsSearchDestination
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsViewModel

@Composable
fun SettingsSearchScreen(
  onSettingSelected: (SettingsSearchEntry) -> Unit,
  viewModel: SettingsViewModel = hiltViewModel(),
) {
  val settingsUiState by viewModel.uiState.collectAsStateWithLifecycle()
  var textFieldValue by
    rememberSaveable(stateSaver = TextFieldValue.Saver) {
      mutableStateOf(TextFieldValue())
    }
  val query = textFieldValue.text
  val listState = rememberLazyListState()
  val focusRequester = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current
  val index =
    rememberSettingsSearchIndex(
      canAccessPodcastSettings = settingsUiState.canDelete,
      canManagePodcastSettings = settingsUiState.canDelete && settingsUiState.isAdmin,
    )
  val results = remember(index, query) { index.search(query) }
  val searchTitle = stringResource(R.string.settings_search)

  LaunchedEffect(Unit) { focusRequester.requestFocus() }

  SettingsSearchScreenContent(
    textFieldValue = textFieldValue,
    results = results,
    searchTitle = searchTitle,
    listState = listState,
    focusRequester = focusRequester,
    onQueryChanged = { textFieldValue = it },
    onSearch = { keyboardController?.hide() },
    onSettingSelected = { entry ->
      if (
        canSelectSettingsSearchEntry(
          entry = entry,
          canAccessPodcastSettings = settingsUiState.canDelete,
          canManagePodcastSettings = settingsUiState.isAdmin,
        )
      ) {
        keyboardController?.hide()
        focusManager.clearFocus()
        onSettingSelected(entry)
      }
    },
  )
}

@Composable
private fun SettingsSearchScreenContent(
  textFieldValue: TextFieldValue,
  results: List<SettingsSearchEntry>,
  searchTitle: String,
  listState: LazyListState,
  focusRequester: FocusRequester,
  onQueryChanged: (TextFieldValue) -> Unit,
  onSearch: () -> Unit,
  onSettingSelected: (SettingsSearchEntry) -> Unit,
) {
  val query = textFieldValue.text

  Column(
    modifier = Modifier.fillMaxSize().imePadding(),
    verticalArrangement = Arrangement.Bottom,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = searchTitle, style = MaterialTheme.typography.titleLarge)
    }

    if (query.isBlank()) {
      SearchMessage(
        text = stringResource(R.string.settings_search_guidance),
        modifier = Modifier.weight(1f),
      )
    } else if (results.isEmpty()) {
      SearchMessage(
        text = stringResource(R.string.settings_search_no_results),
        modifier = Modifier.weight(1f),
      )
    } else {
      LazyColumn(
        state = listState,
        modifier =
          Modifier.weight(1f).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
      ) {
        item(key = "result-count") {
          SettingsSearchResultCountItem(count = results.size)
        }
        items(results, key = SettingsSearchEntry::id) { entry ->
          SettingsSearchResultItem(
            title = entry.title,
            path = entry.path.joinToString(" · "),
            onClick = { onSettingSelected(entry) },
          )
        }
      }
    }

    OutlinedTextField(
      value = textFieldValue,
      onValueChange = onQueryChanged,
      modifier =
        Modifier.fillMaxWidth()
          .padding(horizontal = 16.dp)
          .padding(bottom = 16.dp)
          .focusRequester(focusRequester),
      placeholder = { Text(stringResource(R.string.settings_search_placeholder)) },
      leadingIcon = {
        Icon(
          painter = painterResource(R.drawable.search),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
        )
      },
      trailingIcon = {
        if (query.isNotEmpty()) {
          Icon(
            painter = painterResource(R.drawable.close),
            contentDescription = stringResource(R.string.clear_search),
            modifier =
              Modifier.clickable { onQueryChanged(TextFieldValue()) }.padding(8.dp).size(20.dp),
          )
        }
      },
      singleLine = true,
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    )
  }
}

internal fun canSelectSettingsSearchEntry(
  entry: SettingsSearchEntry,
  canAccessPodcastSettings: Boolean,
  canManagePodcastSettings: Boolean,
): Boolean =
  when (entry.id) {
    SettingsSearchSettingId.PERMANENT_DELETE,
    SettingsSearchSettingId.PODCAST_PERMANENT_DELETE,
    SettingsSearchSettingId.AUTO_SELECT_FINISHED_EPISODES -> canAccessPodcastSettings
    SettingsSearchSettingId.HIDE_DOWNLOADED_EPISODES ->
      canAccessPodcastSettings && canManagePodcastSettings
    else -> entry.destination != SettingsSearchDestination.PODCAST || canAccessPodcastSettings
  }

@Composable
private fun SearchMessage(text: String, modifier: Modifier = Modifier) {
  Column(
    modifier = modifier.fillMaxWidth().padding(24.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SettingsSearchScreenPreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsSearchScreenContent(
      textFieldValue = TextFieldValue(),
      results = emptyList(),
      searchTitle = stringResource(R.string.settings_search),
      listState = rememberLazyListState(),
      focusRequester = remember { FocusRequester() },
      onQueryChanged = {},
      onSearch = {},
      onSettingSelected = {},
    )
  }
}
