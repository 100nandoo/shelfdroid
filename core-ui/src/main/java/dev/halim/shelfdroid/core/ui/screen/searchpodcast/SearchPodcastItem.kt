package dev.halim.shelfdroid.core.ui.screen.searchpodcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Explicit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.searchpodcast.SearchPodcastUi
import dev.halim.shelfdroid.core.navigation.PodcastFeedNavPayload
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SearchPodcastItem(
  model: SearchPodcastUi,
  onClick: (PodcastFeedNavPayload) -> Unit = { _ -> },
  onAddedClick: (String) -> Unit = { _ -> },
) {
  Row(
    modifier =
      Modifier.padding(vertical = 12.dp, horizontal = 16.dp)
        .clickable(
          onClick = { if (model.isAdded) onAddedClick(model.id) else onClick(model.payload) }
        )
  ) {
    CoverNoAnimation(
      Modifier.height(60.dp).padding(end = 16.dp),
      coverUrl = model.cover,
      shape = RoundedCornerShape(4.dp),
    )
    Column(modifier = Modifier.width(IntrinsicSize.Min).weight(1f)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = model.title,
          style = MaterialTheme.typography.bodyMedium,
          maxLines = 2,
          modifier = Modifier.weight(1f),
          overflow = TextOverflow.Ellipsis,
        )
        if (model.explicit) {
          Icon(
            modifier = Modifier.padding(start = 4.dp).size(16.dp),
            imageVector = Icons.Filled.Explicit,
            contentDescription = stringResource(R.string.explicit),
            tint = MaterialTheme.colorScheme.onErrorContainer,
          )
        }
        if (model.isAdded) {
          Icon(
            modifier = Modifier.padding(start = 4.dp).size(16.dp),
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = stringResource(R.string.already_in_library),
            tint = MaterialTheme.colorScheme.tertiaryContainer,
          )
        }
      }
      Text(
        text = model.author,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = model.genre,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = "${model.episodeCount} episodes",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
private fun SearchPodcastItemPreview() {
  PreviewWrapper {
    Column {
      SearchPodcastItem(model = Defaults.SEARCH_PODCAST_1)
      SearchPodcastItem(model = Defaults.SEARCH_PODCAST_2)
      SearchPodcastItem(model = Defaults.SEARCH_PODCAST_3)
      SearchPodcastItem(model = Defaults.SEARCH_PODCAST_4)
    }
  }
}
