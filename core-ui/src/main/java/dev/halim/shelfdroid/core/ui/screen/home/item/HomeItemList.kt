package dev.halim.shelfdroid.core.ui.screen.home.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.components.orchestrators.LibraryItemHeader
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_BOOKS
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_PODCASTS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LazyGridItemScope.HomeItemList(
  id: String,
  title: String,
  author: String,
  cover: String,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  unfinishedEpisodeCount: Int = 0,
) {
  LibraryItemHeader(
    Modifier.animateItem().padding(horizontal = 16.dp),
    id,
    title,
    author,
    cover,
    onClick,
    onLongClick,
    unfinishedEpisodeCount,
  )
}

@ShelfDroidPreview
@Composable
fun PreviewHomeBookItemList() {
  AnimatedPreviewWrapper {
    LazyVerticalGrid(
      columns = GridCells.Fixed(1),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      items(items = HOME_BOOKS, key = { it.id }) { book ->
        HomeItemList(
          id = book.id,
          title = book.title,
          author = book.author,
          cover = book.cover,
          onClick = {},
          onLongClick = {},
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun PreviewHomePodcastItemList() {
  AnimatedPreviewWrapper {
    LazyVerticalGrid(
      columns = GridCells.Fixed(1),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      items(items = HOME_PODCASTS, key = { it.id }) { podcast ->
        HomeItemList(
          id = podcast.id,
          title = podcast.title,
          author = podcast.author,
          cover = podcast.cover,
          onClick = {},
          onLongClick = {},
          unfinishedEpisodeCount = podcast.unfinishedCount,
        )
      }
    }
  }
}
