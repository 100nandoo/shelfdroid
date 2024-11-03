package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.ui.screens.home.BookUiState
import dev.halim.shelfdroid.ui.screens.home.HomeLibraryItemUiState

@Composable
fun HomeLibraryItem(
    uiState: HomeLibraryItemUiState,
    showNoCover: Boolean,
    onImageError: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.padding(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
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
                    contentAlignment = Alignment.Center
                ) {
                    Text(
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