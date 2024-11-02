package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.network.Api

@Composable
fun LibraryItemNoCover() {
    Box(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(Color.Blue, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Invalid cover",
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
fun LibraryItemCover(
    itemId: String,
    onError: () -> Unit,
    modifier: Modifier = Modifier
) {
    AsyncImage(
        model = Api.generateItemCoverUrl(itemId),
        contentDescription = "Library item cover image",
        modifier = modifier
            .padding(4.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp)),
        onError = { onError() }
    )
}