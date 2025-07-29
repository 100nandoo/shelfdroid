package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun UnreadEpisodeCount(modifier: Modifier = Modifier, count: Int = 0) {
  if (count < 1) return
  Box(
    modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = count.toString(),
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onPrimary,
    )
  }
}

@ShelfDroidPreview
@Composable
fun ProgressCirclePreview() {
  PreviewWrapper { UnreadEpisodeCount(modifier = Modifier.size(56.dp)) }
}
