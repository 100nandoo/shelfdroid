package dev.halim.shelfdroid.core.ui.screen.searchpodcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUi
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUiState
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SearchPodcastScreen(
  viewModel: SearchPodcastViewModel = hiltViewModel(),
  onItemClicked: (String, String) -> Unit,
  onAddedClick: (String) -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SearchPodcastScreenContent(
    viewModel::onEvent,
    uiState,
    viewModel.libraryId,
    onItemClicked,
    onAddedClick,
  )
}

@Composable
private fun SearchPodcastScreenContent(
  onEvent: (SearchPodcastEvent) -> Unit = {},
  uiState: SearchPodcastUiState,
  libraryId: String = "",
  onItemClicked: (String, String) -> Unit = { _, _ -> },
  onAddedClick: (String) -> Unit = { _ -> },
) {
  var textFieldValue by rememberSaveable { mutableStateOf("") }

  Column {
    AnimatedVisibility(uiState.state is SearchState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    LazyColumn(modifier = Modifier.imePadding().fillMaxSize(), reverseLayout = true) {
      item(key = "search_text_field") {
        SearchTextField(
          textFieldValue = textFieldValue,
          onValueChange = { textFieldValue = it },
          onEvent = onEvent,
        )
      }
      items(items = uiState.result, key = { it.itunesId }) {
        SearchPodcastItem(it, libraryId, onItemClicked, onAddedClick)
        HorizontalDivider()
      }
    }
  }
}

@Composable
private fun SearchTextField(
  textFieldValue: String,
  onValueChange: (String) -> Unit,
  onEvent: (SearchPodcastEvent) -> Unit,
) {
  val searchRef = remember { FocusRequester() }
  val focusManager = LocalFocusManager.current
  var hasRequestedFocus by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (!hasRequestedFocus) {
      hasRequestedFocus = true
      searchRef.requestFocus()
    }
  }

  Row(Modifier.padding(16.dp)) {
    val onSearchTriggered = {
      if (textFieldValue.isNotBlank()) {
        onEvent(SearchPodcastEvent.Search(textFieldValue))
        focusManager.clearFocus()
      }
    }

    OutlinedTextField(
      value = textFieldValue,
      onValueChange = onValueChange,
      placeholder = { Text("Search term or RSS feel URL") },
      modifier = Modifier.fillMaxWidth().focusRequester(searchRef),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { onSearchTriggered() }),
      trailingIcon = {
        Icon(
          imageVector = Icons.Filled.Search,
          contentDescription = stringResource(R.string.search_podcast),
          modifier = Modifier.clickable { onSearchTriggered() },
        )
      },
      singleLine = true,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SearchPodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SearchPodcastScreenContent(
      uiState =
        SearchPodcastUiState(
          state = SearchState.Success,
          result =
            listOf(SearchPodcastUi("", 1, "NPR", "Planet Money", genre = "Finance, News, Business")),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SearchPodcastScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) {
    SearchPodcastScreenContent(
      uiState =
        SearchPodcastUiState(
          state = SearchState.Success,
          result =
            listOf(SearchPodcastUi("", 2, "NPR", "Planet Money", genre = "Finance, News, Business")),
        )
    )
  }
}
