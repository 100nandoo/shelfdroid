package dev.halim.shelfdroid.core.ui.screen.apikeys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeysUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.API_KEYS_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun ApiKeysScreen(
  viewModel: ApiKeysViewModel = hiltViewModel(),
  onEditClicked: (String) -> Unit = {},
  onDeleteClicked: (String) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ApiKeysContent(
    uiState = uiState,
    onEditClicked = onEditClicked,
    onDeleteClicked = onDeleteClicked,
  )
}

@Composable
private fun ApiKeysContent(
  uiState: ApiKeysUiState = ApiKeysUiState(),
  onEditClicked: (String) -> Unit = {},
  onDeleteClicked: (String) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
    }

    AnimatedVisibility(uiState.apiKeys.isEmpty() && uiState.state is GenericState.Success) {
      GenericMessageScreen(stringResource(R.string.empty_type, stringResource(R.string.api_keys)))
    }

    AnimatedVisibility(uiState.apiKeys.isNotEmpty()) {
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = true,
      ) {
        items(uiState.apiKeys, key = { it.id }) { apiKey ->
          HorizontalDivider()
          ApiKeyItem(
            apiKey = apiKey,
            onEditClicked = { onEditClicked(apiKey.id) },
            onDeleteClicked = { onDeleteClicked(apiKey.id) },
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun ApiKeysScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { ApiKeysContent(uiState = API_KEYS_UI_STATE) }
}
