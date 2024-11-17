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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlayerScreen(paddingValues: PaddingValues) {
    val viewModel = koinViewModel<PlayerViewModel>()
    val uiState by viewModel.uiState.collectAsState()

    PlayerScreenContent(paddingValues = paddingValues)
}

@Composable
fun PlayerScreenContent(
    paddingValues: PaddingValues = PaddingValues(),
    imageUrl: String = "",
    title: String = "Chapter 26",
    authorName: String = "Adam",
    progress: Float = 0f,
    onProgressChange: (Float) -> Unit = {},
    onImageError: () -> Unit = {  },
    onSeekBack: () -> Unit = {},
    onSeekForward: () -> Unit= {},
    isPlaying: Boolean = false,
    onPlayPauseToggle: () -> Unit= {},
    playbackSpeed: Float = 0f,
    onPlaybackSpeedChange: () -> Unit= {},
    onSleepTimer: () -> Unit= {},
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

        BasicPlayerControl(onSeekBack, onPlayPauseToggle, isPlaying, onSeekForward)
        Spacer(modifier = Modifier.height(16.dp))

        AdvancedPlayerControl(onPlaybackSpeedChange, playbackSpeed, onSleepTimer)
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
    onPlaybackSpeedChange: () -> Unit,
    playbackSpeed: Float,
    onSleepTimer: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onPlaybackSpeedChange) {
            Text(text = "Speed: ${playbackSpeed}x")
        }

        TextButton(onClick = onSleepTimer) {
            Text(text = "Sleep Timer")
        }
    }
}

@Composable
fun BasicPlayerControl(
    onSeekBack: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    isPlaying: Boolean,
    onSeekForward: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        IconButton(onClick = onSeekBack) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Seek Back 10s"
            )
        }

        IconButton(onClick = onPlayPauseToggle) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play"
            )
        }

        IconButton(onClick = onSeekForward) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Seek Forward 10s"
            )
        }
    }
}