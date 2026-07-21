@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.rssfeeds

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextLabelValue
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.Defaults.RSS_FEED
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState

@Composable
fun RssFeedSheet(
  sheetState: SheetState,
  feed: RssFeedsUiState.RssFeedUi,
  onDismiss: () -> Unit,
  onCopyUrl: (String) -> Unit,
) {
  ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
    LazyColumn(modifier = Modifier.padding(bottom = 64.dp)) {
      item {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
          CoverNoAnimation(
            modifier = Modifier.size(88.dp),
            coverUrl = feed.coverUrl,
            shape = RoundedCornerShape(8.dp),
            showFallback = true,
          )
          Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
            TextTitleMedium(text = feed.title)
            TextLabelSmall(
              text = entityTypeLabel(feed.entityType),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextLabelSmall(
              text =
                "${feed.episodeCount} ${pluralStringResource(R.plurals.plurals_episode, feed.episodeCount)}",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))

        OutlinedTextField(
          value = feed.publicFeedUrl,
          onValueChange = {},
          readOnly = true,
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
          label = { Text(stringResource(R.string.public_feed_url)) },
          singleLine = true,
          trailingIcon = {
            IconButton(onClick = { onCopyUrl(feed.publicFeedUrl) }) {
              Icon(
                painter = painterResource(R.drawable.copy),
                contentDescription = stringResource(R.string.copy_rss_url),
              )
            }
          },
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
          TextLabelValue(
            label = stringResource(R.string.last_updated),
            value = feed.updatedAtText,
            labelWeight = 2f,
          )
          TextLabelValue(
            label = stringResource(R.string.edit_item_type),
            value = entityTypeLabel(feed.entityType),
            labelWeight = 2f,
          )
          TextLabelValue(
            label = stringResource(R.string.rss_feed_slug),
            value = feed.slug,
            labelWeight = 2f,
          )
          TextLabelValue(
            label = stringResource(R.string.rss_feed_prevent_indexing),
            value =
              if (feed.preventIndexing) stringResource(R.string.yes)
              else stringResource(R.string.no),
            labelWeight = 2f,
          )
          TextLabelValue(
            label = stringResource(R.string.rss_feed_owner_name),
            value = feed.ownerName.orEmpty(),
            labelWeight = 2f,
          )
          TextLabelValue(
            label = stringResource(R.string.rss_feed_owner_email),
            value = feed.ownerEmail.orEmpty(),
            labelWeight = 2f,
          )
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextTitleMedium(
          modifier = Modifier.padding(horizontal = 16.dp),
          text = stringResource(R.string.episodes),
        )
      }

      itemsIndexed(feed.episodes) { index, episode ->
        if (index > 0) HorizontalDivider()
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
          TextBodyMedium(text = episode.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
      }
    }
  }
}

@Composable
internal fun entityTypeLabel(entityType: String): String {
  return when (entityType) {
    "libraryItem" -> stringResource(R.string.library_item)
    "series" -> stringResource(R.string.edit_item_series)
    "collection" -> stringResource(R.string.collection)
    else -> stringResource(R.string.unknown_title)
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewRssBottomSheet() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val sheetState = sheetState(density)

    RssFeedSheet(
      sheetState = sheetState,
      feed = RSS_FEED,
      onDismiss = {},
      onCopyUrl = {},
    )
    LaunchedEffect(Unit) { sheetState.show() }
  }
}
