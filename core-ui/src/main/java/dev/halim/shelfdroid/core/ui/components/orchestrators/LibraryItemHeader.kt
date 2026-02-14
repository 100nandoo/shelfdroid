package dev.halim.shelfdroid.core.ui.components.orchestrators

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.Cover
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.HOME_BOOKS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.UnreadEpisodeCount

@Composable
fun LibraryItemHeader(
  modifier: Modifier,
  id: String,
  title: String,
  author: String,
  cover: String,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
  unfinishedEpisodeCount: Int = 0,
) {
  val isClickable = onClick != null && onLongClick != null

  val sharedTransitionScope = LocalSharedTransitionScope.current

  with(sharedTransitionScope) {
    Row(
      modifier =
        Modifier.height(88.dp)
          .fillMaxWidth()
          .mySharedBound(Animations.containerKey(id))
          .then(
            if (isClickable) {
              Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
            } else Modifier
          )
          .then(modifier)
          .padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Cover(
        Modifier.fillMaxHeight().padding(end = 12.dp),
        cover = cover,
        shape = RoundedCornerShape(4.dp),
        animationKey = Animations.coverKey(id),
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
          modifier = Modifier.mySharedBound(Animations.authorKey(id, author)).skipToLookaheadSize(),
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

@ShelfDroidPreview
@Composable
fun LibraryItemHeaderList() {
  AnimatedPreviewWrapper {
    val book = HOME_BOOKS.first()
    Column {
      LibraryItemHeader(
        modifier = Modifier,
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
