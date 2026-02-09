package dev.halim.shelfdroid.core.ui.screen.listeningsession

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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ListeningSessionScreen(viewModel: ListeningSessionViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ListeningSessionContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun ListeningSessionContent(
  uiState: ListeningSessionUiState = ListeningSessionUiState(),
  onEvent: (ListeningSessionEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    LazyColumn(modifier = Modifier.weight(1f)) {
      items(uiState.sessions, key = { it.id }) { session ->
        ListeningSessionItem(session)
        HorizontalDivider()
      }
    }
    Spacer(modifier = Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun ListeningSessionScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { ListeningSessionContent() }
}
