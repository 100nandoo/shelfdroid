package dev.halim.shelfdroid.ui.screens.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.ui.components.LibraryItemPlayIcon
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun PlayerScreen(paddingValues: PaddingValues, id: String) {
    val viewModel: PlayerViewModel = koinViewModel(parameters = { parametersOf(id) })
    val uiState by viewModel.uiState.collectAsState()
    val bookPlayerUiState by remember(uiState) { derivedStateOf { uiState.bookPlayerUiState } }
    PlayerScreenContent(
        paddingValues = paddingValues,
        bookPlayerUiState,
        bookPlayerUiState.cover,
        bookPlayerUiState.currentChapter.title,
        bookPlayerUiState.author,
    )
}

@Composable
fun PlayerScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    bookUiState: BookPlayerUiState = BookPlayerUiState(),
    imageUrl: String = "",
    title: String = "Chapter 26",
    authorName: String = "Adam",
    progress: Float = 0f,
    onProgressChange: (Float) -> Unit = {},
    onImageError: () -> Unit = { },
    onSleepTimer: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicPlayerContent(imageUrl, onImageError, title, authorName)
        Spacer(modifier = Modifier.height(16.dp))

        // Progress Slider
        Slider(
            value = progress,
            onValueChange = onProgressChange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        BasicPlayerControl(bookUiState)
        Spacer(modifier = Modifier.height(16.dp))

        AdvancedPlayerControl(onSleepTimer)
    }
}

@Composable
fun BasicPlayerContent(
    imageUrl: String,
    onImageError: () -> Unit,
    title: String,
    authorName: String,
) {
    // Episode Image with AsyncImage
    AsyncImage(
        model = imageUrl,
        contentDescription = "Library item cover image",
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .aspectRatio(1f),
        onError = { onImageError() }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // Episode Title
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    // Author Name
    Text(
        text = authorName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )

}

@Composable
fun AdvancedPlayerControl(
    onSleepTimer: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        StepsSlider()

        TextButton(onClick = onSleepTimer) {
            Text(text = "Sleep Timer")
        }
    }
}

@Composable
fun BasicPlayerControl(bookPlayerUiState: BookPlayerUiState) {
    val mediaManager = koinInject<MediaManager>()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = { mediaManager.seekBackward() }) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek Back 10s"
            )
        }

        LibraryItemPlayIcon({ mediaManager.playBookUiState(bookPlayerUiState.toImpl()) }, bookPlayerUiState.id)

        IconButton(onClick = { mediaManager.seekForward() }) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek Forward 10s"
            )
        }
    }
}

@Composable
fun StepsSlider() {
    val mediaManager = koinInject<MediaManager>()
    var sliderPosition by remember { mutableStateOf(1f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Speed: ${sliderPosition}x")
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            modifier = Modifier
                .semantics { contentDescription = "speed slider" }
                .width(150.dp),
            value = sliderPosition,
            onValueChange = { sliderPosition = it },
            valueRange = 0.5f..2f,
            onValueChangeFinished = {
                mediaManager.changeSpeed(sliderPosition)
            },
            steps = 5,
        )

    }
}