package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.expect.MediaPlayerState
import dev.halim.shelfdroid.expect.PlaybackState
import dev.halim.shelfdroid.ui.ShelfdroidMediaItem
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeEvent

@Composable
fun HomeItem(
    uiState: ShelfdroidMediaItem,
    modifier: Modifier = Modifier,
    playerState: MediaPlayerState = MediaPlayerState(),
    onEvent: (HomeEvent) -> Unit = {},
) {
    Card(
        modifier = modifier.padding(4.dp),
        onClick = { onEvent(HomeEvent.Navigate(uiState.id, uiState is BookUiState)) },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                ItemCover(uiState.cover)
                if (uiState is BookUiState) {
                    ItemPlayIcon(
                        uiState,
                        { onEvent(HomeEvent.PlayBook(uiState)) },
                        playerState
                    )
                }
            }
            if (uiState is BookUiState) {
                if (uiState.progress > 0.0) {
                    ItemProgressIndicator(progress = uiState.progress)
                } else {
                    Box(modifier = Modifier.padding(bottom = 4.dp))
                }
            }

            Text(
                text = uiState.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 8.dp)
            )

            Text(
                text = uiState.author,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
fun ItemCover(coverUrl: String, shape: Shape = RoundedCornerShape(8.dp, 8.dp)) {
    var imageLoadFailed by remember { mutableStateOf(false) }
    if (imageLoadFailed) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    shape = shape
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "No cover",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center
            )
        }

    } else {
        AsyncImage(
            model = coverUrl,
            contentDescription = "Library item cover image",
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .aspectRatio(1f),
            onError = { imageLoadFailed = true }
        )
    }

}

@Composable
fun ItemPlayIcon(
    mediaItem: ShelfdroidMediaItem,
    onPlay: (ShelfdroidMediaItem) -> Unit,
    playerState: MediaPlayerState,
    itemPlayIconSize: ItemPlayIconSize = ItemPlayIconSize.Large
) {
    val (buttonSize, iconSize) = when (itemPlayIconSize) {
        ItemPlayIconSize.Small -> 48.dp to 36.dp
        ItemPlayIconSize.Large -> 60.dp to 48.dp
    }
    val isCurrentMediaItem = playerState.itemId == mediaItem.id
    val icon = when (playerState.playbackState) {
        PlaybackState.Buffering -> if (isCurrentMediaItem) Icons.Default.HourglassTop else Icons.Default.PlayArrow
        PlaybackState.Playing -> {
            if (isCurrentMediaItem) Icons.Default.Pause else Icons.Default.PlayArrow
        }

        PlaybackState.Pause -> Icons.Default.PlayArrow
        else -> Icons.Default.PlayArrow
    }
    OutlinedButton(
        onClick = { onPlay(mediaItem) },
        shape = CircleShape,
        modifier = Modifier.size(buttonSize),
        contentPadding = PaddingValues(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Play or Pause",
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier
                .size(iconSize)
        )
    }
}

sealed class ItemPlayIconSize {
    data object Small : ItemPlayIconSize()
    data object Large : ItemPlayIconSize()
}