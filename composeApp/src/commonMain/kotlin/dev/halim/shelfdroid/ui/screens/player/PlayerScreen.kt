package dev.halim.shelfdroid.ui.screens.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.ui.ShelfdroidMediaItemImpl
import dev.halim.shelfdroid.ui.components.ItemCover
import dev.halim.shelfdroid.ui.components.LibraryItemPlayIcon
import dev.halim.shelfdroid.utility.formatTime
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Composable
fun PlayerScreen(paddingValues: PaddingValues, id: String) {
    val viewModel: PlayerViewModel = koinViewModel(parameters = { parametersOf(id) })

    val uiState by viewModel.uiState.collectAsStateWithLifecycle(PlayerUiState())
    val progressUiState by viewModel.playerProgressUiState.collectAsStateWithLifecycle(PlayerProgressUiState())
    val advanceUiState by viewModel.advanceUiState.collectAsStateWithLifecycle(AdvanceUiState())
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()

    if (uiState.state == PlayerState.Success) {
        val bookPlayerUiState by remember(uiState) { derivedStateOf { uiState.bookPlayerUiState } }
        PlayerScreenContent(
            paddingValues = paddingValues,
            bookPlayerUiState,
            progressUiState,
            playerState,
            bookPlayerUiState.cover,
            bookPlayerUiState.currentChapter.title,
            bookPlayerUiState.author,
            advanceUiState.speed,
            onEvent = { event -> viewModel.onEvent(event) }
        )
    }
}

@Composable
fun PlayerScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    uiState: BookPlayerUiState = BookPlayerUiState(),
    progressUiState: PlayerProgressUiState = PlayerProgressUiState(),
    playerState: MediaPlayerState = MediaPlayerState(),
    imageUrl: String = "",
    title: String = "Chapter 26",
    authorName: String = "Adam",
    speed: Float = 1f,
    onEvent: (PlayerEvent) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BasicPlayerContent(imageUrl, title, authorName)
        Spacer(modifier = Modifier.height(16.dp))

        PlayerProgress(progressUiState, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        BasicPlayerControl(uiState.toImpl(), playerState, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        AdvancedPlayerControl(speed, onEvent)
    }
}

@Composable
fun PlayerProgress(
    uiState: PlayerProgressUiState,
    onEvent: (PlayerEvent) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = formatTime(uiState.currentTime.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = formatTime(uiState.totalTime.toLong()),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
    Slider(
        value = uiState.progress,
        onValueChange = { onEvent(PlayerEvent.ProgressChanged(it)) },
        modifier = Modifier.fillMaxWidth(),
        colors = SliderDefaults.colors(
            thumbColor = MaterialTheme.colorScheme.primary,
            activeTrackColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun BasicPlayerContent(
    url: String,
    title: String,
    authorName: String,
) {
    ItemCover(url, RoundedCornerShape(8.dp))

    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = authorName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )

}

@Composable
fun AdvancedPlayerControl(
    speed: Float,
    onEvent: (PlayerEvent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        StepsSlider(speed, onEvent)

        TextButton(onClick = { onEvent(PlayerEvent.SetSleepTimer(10.seconds)) }) {
            Text(text = "Sleep Timer")
        }
    }
}

@Composable
fun BasicPlayerControl(
    shelfdroidMediaItemImpl: ShelfdroidMediaItemImpl,
    playerState: MediaPlayerState,
    onEvent: (PlayerEvent) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.clip(CircleShape)
            .clickable { onEvent(PlayerEvent.SeekBack) }
            .size(48.dp)) {
            Icon(
                modifier = Modifier.size(36.dp).align(Alignment.Center),
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek Back 10s"
            )
        }

        LibraryItemPlayIcon(
            shelfdroidMediaItemImpl,
            { onEvent(PlayerEvent.PlayBook) },
            playerState
        )

        Box(modifier = Modifier.clip(CircleShape)
            .clickable { onEvent(PlayerEvent.SeekForward) }
            .size(48.dp)) {
            Icon(
                modifier = Modifier.size(36.dp).align(Alignment.Center),
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek Forward 10s"
            )
        }
    }
}

@Composable
fun StepsSlider(speed: Float, onEvent: (PlayerEvent) -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Speed: ${speed}x")
        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            modifier = Modifier
                .semantics { contentDescription = "speed slider" }
                .width(150.dp),
            value = speed,
            onValueChange = { onEvent(PlayerEvent.ChangeSpeed(it)) },
            valueRange = 0.5f..2f,
            steps = 5,
        )
    }
}