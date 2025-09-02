package dev.halim.shelfdroid.core.ui.screen.home.item

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_BOOKS
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_PODCASTS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.ItemCover
import dev.halim.shelfdroid.core.ui.screen.home.UnreadEpisodeCount

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LazyGridItemScope.HomeItemList(
  id: String,
  title: String,
  author: String,
  cover: String,
  onClick: () -> Unit,
  unfinishedEpisodeCount: Int = 0,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Row(
        modifier =
          Modifier.animateItem()
            .height(88.dp)
            .fillMaxWidth()
            .mySharedBound(Animations.containerKey(id))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        ItemCover(
          Modifier.fillMaxHeight().padding(end = 16.dp),
          cover = cover,
          shape = RoundedCornerShape(4.dp),
        )
        Column(Modifier.fillMaxWidth().weight(1f)) {
          Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.mySharedBound(Animations.titleKey(id, title)),
          )
          Text(
            text = author,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier =
              Modifier.mySharedBound(Animations.authorKey(id, author)).skipToLookaheadSize(),
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
  }
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
          unfinishedEpisodeCount = podcast.unfinishedCount,
        )
      }
    }
  }
}
