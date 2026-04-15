package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ChaptersTab(uiState: EditItemUiState) {
  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text(
      text = stringResource(R.string.edit_item_chapters_count, uiState.chapters.size),
      style = MaterialTheme.typography.titleMedium,
    )
    HorizontalDivider()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
      Text(
        stringResource(R.string.edit_item_chapter_number),
        modifier = Modifier.weight(0.5f),
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        stringResource(R.string.title),
        modifier = Modifier.weight(3f),
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        stringResource(R.string.edit_item_chapter_start),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
      )
      Text(
        stringResource(R.string.edit_item_chapter_end),
        modifier = Modifier.weight(1f),
        style = MaterialTheme.typography.labelMedium,
      )
    }
    HorizontalDivider()
    uiState.chapters.forEach { chapter ->
      Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("${chapter.id}", modifier = Modifier.weight(0.5f))
        Text(chapter.title, modifier = Modifier.weight(3f))
        Text(formatSeconds(chapter.start), modifier = Modifier.weight(1f))
        Text(formatSeconds(chapter.end), modifier = Modifier.weight(1f))
      }
    }
  }
}

private fun formatSeconds(seconds: Double): String {
  val total = seconds.toInt()
  val h = total / 3600
  val m = (total % 3600) / 60
  val s = total % 60
  return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@ShelfDroidPreview
@Composable
private fun ChaptersTabPreview() {
  PreviewWrapper { ChaptersTab(uiState = Defaults.EDIT_ITEM_UI_STATE) }
}
