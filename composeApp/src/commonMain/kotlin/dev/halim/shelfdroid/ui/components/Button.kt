package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .size(48.dp)
    ) {
        Icon(
            modifier = Modifier
                .size(36.dp)
                .align(Alignment.Center),
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            imageVector = icon,
            contentDescription = contentDescription
        )
    }
}