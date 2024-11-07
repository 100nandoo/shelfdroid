package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.expect.MediaManager
import dev.halim.shelfdroid.network.Api
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeLibraryItemUiState
import org.koin.compose.koinInject

@Composable
fun HomeLibraryItem(
    uiState: HomeLibraryItemUiState,
    showNoCover: Boolean,
    onImageError: () -> Unit,
    onPlayPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaManager = koinInject<MediaManager>()
    val api = koinInject<Api>()
    var isPlay by remember { mutableStateOf(false) }
    Card(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                if (showNoCover) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp, 8.dp)
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
                        model = uiState.cover,
                        contentDescription = "Library item cover image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp, 8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .aspectRatio(1f),
                        onError = { onImageError() }
                    )
                }

                if (uiState is BookUiState) {
                    Icon(
                        imageVector = if (isPlay) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                            .clickable {
                                if (isPlay) {
                                    mediaManager.pause()
                                } else {
                                    if (mediaManager.currentItem() == null) {
                                        val (ino, target) = uiState.findInoIdAndSeekTiming(api)
                                        mediaManager.addItem(ino, target)
                                    }
                                    mediaManager.play()
                                }
                                isPlay = isPlay.not()
                            }
                    )
                }
            }
            if (uiState is BookUiState) {
                if (uiState.progress > 0.0) {
                    val float = uiState.progress.toFloat()
                    LinearProgressIndicator(
                        progress = { float },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onTertiary,
                        trackColor = MaterialTheme.colorScheme.tertiary
                    )
                } else {
                    Box(modifier = Modifier.padding(bottom = 16.dp))
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