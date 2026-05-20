package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.player.bigplayer.ChapterRow
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ChaptersTab(uiState: EditItemUiState) {
  val context = LocalContext.current
  val openChapterEditor =
    remember(context, uiState.webBaseUrl, uiState.itemId) {
      {
        val url = "${uiState.webBaseUrl}/audiobookshelf/audiobook/${uiState.itemId}/chapters"
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
      }
    }

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Column(
      modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      uiState.chapters.forEachIndexed { index, chapter ->
        ChapterRow(
          title = chapter.title,
          chapterTime = "${formatSeconds(chapter.start)} - ${formatSeconds(chapter.end)}",
          modifier =
            Modifier.background(MaterialTheme.colorScheme.surface).padding(horizontal = 16.dp),
          chapterTitleLine = 2,
        )
        if (index < uiState.chapters.lastIndex) {
          HorizontalDivider()
        }
      }
    }
    Button(onClick = openChapterEditor, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
      Text(stringResource(R.string.edit_item_edit_chapters))
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
