@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home.item

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ListItem
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import dev.halim.shelfdroid.core.ui.screen.home.ItemCoverNoAnimation
import dev.halim.shelfdroid.core.ui.screen.home.UnreadEpisodeCount
import kotlinx.coroutines.launch

@Composable
fun HomeItemBottomSheet(
  sheetState: SheetState,
  isBook: Boolean,
  selectedBook: BookUiState,
  selectedPodcast: PodcastUiState,
  onDelete: () -> Unit = {},
) {
  val scope = rememberCoroutineScope()
  var showDeleteDialog by remember { mutableStateOf(false) }

  val text =
    if (isBook) stringResource(R.string.dialog_delete_book)
    else stringResource(R.string.dialog_delete_podcast)
  MyAlertDialog(
    title = stringResource(R.string.delete),
    text = text,
    showDialog = showDeleteDialog,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onDelete()
      scope.launch { sheetState.hide() }
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
  )

  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = { scope.launch { sheetState.hide() } },
  ) {
    Spacer(modifier = Modifier.height(16.dp))
    ItemDetail(isBook, selectedBook, selectedPodcast)

    HorizontalDivider(modifier = Modifier.padding(16.dp))

    ListItem(
      text = "Delete",
      contentDescription = "Delete",
      icon = R.drawable.delete,
      { showDeleteDialog = true },
    )
    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun ItemDetail(
  isBook: Boolean,
  selectedBook: BookUiState,
  selectedPodcast: PodcastUiState,
) {
  val title: String
  val author: String
  val cover: String
  val unfinishedEpisodeCount: Int
  if (isBook) {
    title = selectedBook.title
    author = selectedBook.author
    cover = selectedBook.cover
    unfinishedEpisodeCount = 0
  } else {
    title = selectedPodcast.title
    author = selectedPodcast.author
    cover = selectedPodcast.cover
    unfinishedEpisodeCount = selectedPodcast.unfinishedCount
  }

  Row(
    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    ItemCoverNoAnimation(
      Modifier.height(64.dp).padding(end = 16.dp),
      coverUrl = cover,
      shape = RoundedCornerShape(4.dp),
    )
    Column(Modifier.fillMaxWidth().weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Text(
        text = author,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Start,
      )
    }
    if (unfinishedEpisodeCount > 0) {
      UnreadEpisodeCount(
        modifier = Modifier.padding(start = 16.dp).size(24.dp),
        count = unfinishedEpisodeCount,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewHomeItemBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val sheetState = sheetState(density)

    HomeItemBottomSheet(
      sheetState = sheetState,
      isBook = true,
      selectedBook = BookUiState(),
      selectedPodcast = PodcastUiState(),
    )
    LaunchedEffect(Unit) { sheetState.show() }
  }
}
