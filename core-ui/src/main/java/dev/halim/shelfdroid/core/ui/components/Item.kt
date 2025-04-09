import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.core.data.home.BookUiState
import dev.halim.shelfdroid.core.data.home.ShelfdroidMediaItem
import dev.halim.shelfdroid.core.ui.screen.home.HomeEvent

@Composable
fun Item(
    uiState: ShelfdroidMediaItem,
    modifier: Modifier = Modifier,
    onEvent: (HomeEvent) -> Unit = {},
) {
    Card(
        modifier = modifier.padding(4.dp),
        onClick = {
            onEvent(HomeEvent.Navigate(uiState.id, uiState is BookUiState))
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                ItemCover(uiState.cover)
            }
            if (uiState is BookUiState) {
                if (uiState.progress > 0.0) {
                    val float = uiState.progress
                    LinearProgressIndicator(
                        progress = { float },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        trackColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        drawStopIndicator = {})
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
fun ItemDetail(
    url: String,
    title: String,
    authorName: String,
    subtitle: String = "",
) {
    Spacer(modifier = Modifier.height(16.dp))
    ItemCover(url, RoundedCornerShape(8.dp))
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center
    )

    if (subtitle.isNotEmpty()) {
        Text(
            text = subtitle,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = authorName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center
    )
}