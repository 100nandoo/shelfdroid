@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadministration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationLibrary
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTextButtonRetry
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdministrationScreen(
  viewModel: LibraryAdministrationViewModel = hiltViewModel(),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  LibraryAdministrationContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
internal fun LibraryAdministrationContent(
  uiState: LibraryAdministrationUiState = LibraryAdministrationUiState(),
  onEvent: (LibraryAdministrationEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = stringResource(R.string.library_administration),
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier.weight(1f),
      )
      IconButton(
        onClick = { onEvent(LibraryAdministrationEvent.Refresh) },
        enabled = !uiState.isRefreshing,
      ) {
        Icon(
          painter = painterResource(R.drawable.refresh),
          contentDescription = stringResource(R.string.refresh_libraries),
        )
      }
    }

    PullToRefreshBox(
      modifier = Modifier.fillMaxWidth().weight(1f),
      isRefreshing = uiState.isRefreshing,
      onRefresh = { onEvent(LibraryAdministrationEvent.Refresh) },
    ) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
      ) {
        when (val state = uiState.state) {
          GenericState.Idle -> Unit

          GenericState.Loading -> {
            item(key = "loading") {
              Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
              ) {
                CircularProgressIndicator()
              }
            }
          }

          is GenericState.Failure -> {
            item(key = "failure") {
              MyTextButtonRetry(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                message = state.errorMessage ?: stringResource(R.string.server_could_not_be_reached),
                onRetry = { onEvent(LibraryAdministrationEvent.Refresh) },
              )
            }
          }

          GenericState.Success -> {
            if (uiState.libraries.isEmpty()) {
              item(key = "empty") {
                Text(
                  text = stringResource(R.string.library_administration_empty),
                  modifier = Modifier.fillMaxWidth().padding(32.dp),
                  style = MaterialTheme.typography.titleLarge,
                )
              }
            } else {
              items(uiState.libraries, key = { it.id }) { library ->
                LibraryAdministrationItem(library)
              }
            }
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdministrationContentPreview() {
  PreviewWrapper {
    LibraryAdministrationContent(
      uiState =
        LibraryAdministrationUiState(
          state = GenericState.Success,
          isRefreshing = false,
          libraries =
            listOf(
              LibraryAdministrationLibrary(
                id = "book-library",
                name = "Books",
                mediaType = LibraryAdministrationMediaType.BOOK,
                displayOrder = 0,
              ),
              LibraryAdministrationLibrary(
                id = "podcast-library",
                name = "Podcasts",
                mediaType = LibraryAdministrationMediaType.PODCAST,
                displayOrder = 1,
              ),
            ),
        )
    )
  }
}

@ShelfDroidPreview
@Composable
private fun LibraryAdministrationFailurePreview() {
  PreviewWrapper {
    LibraryAdministrationContent(
      uiState =
        LibraryAdministrationUiState(
          state = GenericState.Failure("The server could not be reached."),
          isRefreshing = false,
        )
    )
  }
}
