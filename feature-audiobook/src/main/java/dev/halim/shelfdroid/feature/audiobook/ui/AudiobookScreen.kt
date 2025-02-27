package dev.halim.shelfdroid.feature.audiobook.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ui.MyApplicationTheme
import dev.halim.shelfdroid.feature.audiobook.ui.AudiobookUiState.Success

@Composable
fun AudiobookScreen(modifier: Modifier = Modifier, viewModel: AudiobookViewModel = hiltViewModel()) {
    val items by viewModel.uiState.collectAsStateWithLifecycle()
    val baseUrl by viewModel.baseUrl.collectAsStateWithLifecycle()
    if (items is Success) {
        AudiobookScreen(
            items = (items as Success).data,
            baseUrl = baseUrl,
            onSave = { name -> viewModel.addAudiobook(name) },
            modifier = modifier
        )
    }
}

@Composable
internal fun AudiobookScreen(
    items: List<String>,
    baseUrl: String,
    onSave: (name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        var nameAudiobook by remember { mutableStateOf("Compose") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TextField(
                value = nameAudiobook,
                onValueChange = { nameAudiobook = it }
            )

            Button(modifier = Modifier.width(96.dp), onClick = { onSave(nameAudiobook) }) {
                Text("Save")
            }
        }
        Text("Base Url: $baseUrl")
        items.forEach {
            Text("Saved item: $it")
        }
    }
}

// Previews

@Preview(showBackground = true)
@Composable
private fun DefaultPreview() {
    MyApplicationTheme {
        AudiobookScreen(listOf("Compose", "Room", "Kotlin"),  "base url",onSave = {})
    }
}

@Preview(showBackground = true, widthDp = 480)
@Composable
private fun PortraitPreview() {
    MyApplicationTheme {
        AudiobookScreen(listOf("Compose", "Room", "Kotlin"), "base url", onSave = {})
    }
}
