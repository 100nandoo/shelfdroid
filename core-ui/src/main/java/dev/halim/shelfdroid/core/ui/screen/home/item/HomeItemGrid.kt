package dev.halim.shelfdroid.core.ui.screen.home.item

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.Cover
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_BOOKS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.UnreadEpisodeCount

@Composable
fun LazyGridItemScope.HomeItemGrid(
  id: String,
  title: String,
  author: String,
  cover: String,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  unfinishedEpisodeCount: Int = 0,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current

  with(sharedTransitionScope) {
    Card(
      modifier =
        Modifier.animateItem()
          .mySharedBound(Animations.containerKey(id))
          .padding(4.dp)
          .combinedClickable(onLongClick = onLongClick, onClick = onClick),
      shape = RoundedCornerShape(8.dp),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.fillMaxWidth()) {
          Cover(Modifier.fillMaxWidth(), cover = cover)
          if (unfinishedEpisodeCount > 0) {
            UnreadEpisodeCount(
              modifier = Modifier.size(40.dp).padding(8.dp),
              count = unfinishedEpisodeCount,
            )
          }
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurface,
          modifier =
            Modifier.mySharedBound(Animations.titleKey(id, title))
              .padding(horizontal = 8.dp)
              .padding(top = 8.dp),
        )

        Text(
          text = author,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
          modifier =
            Modifier.mySharedBound(Animations.authorKey(id, author))
              .skipToLookaheadSize()
              .padding(horizontal = 8.dp)
              .padding(top = 4.dp, bottom = 8.dp),
        )
      }
    }
  }
}

@Composable
fun ItemDetail(id: String, url: String, title: String, authorName: String, subtitle: String = "") {
  val sharedTransitionScope = LocalSharedTransitionScope.current

  with(sharedTransitionScope) {
    Spacer(modifier = Modifier.height(16.dp))
    Cover(
      Modifier.fillMaxWidth(),
      cover = url,
      shape = RoundedCornerShape(8.dp),
      animationKey = Animations.coverKey(id),
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      modifier = Modifier.mySharedElement(Animations.titleKey(id, title)).skipToLookaheadSize(),
      text = title,
      style = MaterialTheme.typography.headlineLarge,
      textAlign = TextAlign.Center,
    )

    if (subtitle.isNotEmpty()) {
      Text(
        text = subtitle,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(modifier = Modifier.height(8.dp))

    Text(
      modifier =
        Modifier.mySharedElement(Animations.authorKey(id, authorName)).skipToLookaheadSize(),
      text = authorName,
      style = MaterialTheme.typography.bodyMedium,
      color = Color.Gray,
      textAlign = TextAlign.Center,
    )
  }
}

@ShelfDroidPreview
@Composable
fun PreviewHomeItemTwoGrid() {
  AnimatedPreviewWrapper {
    LazyVerticalGrid(
      columns = GridCells.Fixed(2),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      items(items = HOME_BOOKS, key = { it.id }) { book ->
        HomeItemGrid(
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
fun PreviewHomeItemThreeGrid() {
  AnimatedPreviewWrapper {
    LazyVerticalGrid(
      columns = GridCells.Fixed(3),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      items(items = HOME_BOOKS, key = { it.id }) { book ->
        HomeItemGrid(
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
