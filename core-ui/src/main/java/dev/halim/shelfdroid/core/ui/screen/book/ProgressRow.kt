package dev.halim.shelfdroid.core.ui.screen.book

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ProgressRow(progress: Int, remaining: String) {
  val isComplete = progress >= 100

  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
  ) {
    if (isComplete) {
      Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(R.string.completed),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    } else {
      Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          modifier = Modifier.weight(1f),
          text = stringResource(R.string.args_percent, progress),
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          modifier = Modifier.weight(4f),
          text = stringResource(R.string.args_remaining, remaining),
          textAlign = TextAlign.Start,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeItemCompletedPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
      item { ProgressRow(Defaults.PROGRESS_COMPLETE_PERCENT, Defaults.BOOK_REMAINING) }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeItemPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
      item { ProgressRow(Defaults.PROGRESS_PERCENT, Defaults.BOOK_REMAINING) }
    }
  }
}

@ShelfDroidPreview
@Composable
fun EpisodeItemDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
      item { ProgressRow(Defaults.PROGRESS_PERCENT, Defaults.BOOK_REMAINING) }
    }
  }
}
