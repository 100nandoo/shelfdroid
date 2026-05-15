package dev.halim.shelfdroid.core.ui.screen.home.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_BOOKS
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_PODCASTS
import dev.halim.shelfdroid.core.ui.preview.LazyGridItemPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LazyGridItemScope.HomeItem(
  listView: Boolean = false,
  id: String,
  title: String,
  author: String,
  cover: String,
  unfinishedEpisodeCount: Int = 0,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  if (listView) {
    HomeItemList(
      id = id,
      title = title,
      author = author,
      cover = cover,
      unfinishedEpisodeCount = unfinishedEpisodeCount,
      onClick = onClick,
      onLongClick = onLongClick,
    )
  } else {
    HomeItemGrid(
      id = id,
      title = title,
      author = author,
      cover = cover,
      unfinishedEpisodeCount = unfinishedEpisodeCount,
      onClick = onClick,
      onLongClick = onLongClick,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun HomeItemGridPreview() {
  val book = HOME_BOOKS.first()

  LazyGridItemPreviewWrapper(
    columns = GridCells.Fixed(2),
    animated = true,
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
  ) {
    HomeItem(
      id = book.id,
      title = book.title,
      author = book.author,
      cover = book.cover,
      onClick = {},
      onLongClick = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun HomeItemListPreview() {
  val podcast = HOME_PODCASTS.first()

  LazyGridItemPreviewWrapper(
    columns = GridCells.Fixed(1),
    animated = true,
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
  ) {
    HomeItem(
      listView = true,
      id = podcast.id,
      title = podcast.title,
      author = podcast.author,
      cover = podcast.cover,
      unfinishedEpisodeCount = podcast.unfinishedCount,
      onClick = {},
      onLongClick = {},
    )
  }
}
