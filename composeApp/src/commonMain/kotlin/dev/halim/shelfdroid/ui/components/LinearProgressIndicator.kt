package dev.halim.shelfdroid.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ItemProgressIndicator(modifier: Modifier = Modifier.fillMaxWidth(), progress: Float = 0f) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        trackColor = MaterialTheme.colorScheme.onTertiaryContainer,
        drawStopIndicator = {})
}