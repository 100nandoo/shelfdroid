package ${PACKAGE_NAME}

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
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
fun ${NAME}Screen(
    viewModel: ${NAME}ViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ${NAME}Content(
        uiState = uiState,
        onEvent = viewModel::onEvent
    )
}

@Composable
private fun ${NAME}Content(
    uiState: ${NAME}UiState = ${NAME}UiState(),
    onEvent: (${NAME}Event) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize(),
            verticalArrangement = Arrangement.Bottom,
    ) {
        // TODO: Add your UI here

        VisibilityDown(uiState.state is GenericState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        Text("${NAME}Screen")
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@ShelfDroidPreview
@Composable
fun ${NAME}ScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { ${NAME}Content() }
}
