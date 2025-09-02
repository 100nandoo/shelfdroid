package dev.halim.shelfdroid.core.ui.screen.home.item

import HomeItemGrid
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.runtime.Composable

@Composable
fun LazyGridItemScope.HomeItem(
  listView: Boolean = false,
  id: String,
  title: String,
  author: String,
  cover: String,
  unfinishedEpisodeCount: Int = 0,
  onClick: () -> Unit,
) {
  if (listView) {
    HomeItemList(
      id = id,
      title = title,
      author = author,
      cover = cover,
      unfinishedEpisodeCount = unfinishedEpisodeCount,
      onClick = onClick,
    )
  } else {
    HomeItemGrid(
      id = id,
      title = title,
      author = author,
      cover = cover,
      unfinishedEpisodeCount = unfinishedEpisodeCount,
      onClick = onClick,
    )
  }
}
