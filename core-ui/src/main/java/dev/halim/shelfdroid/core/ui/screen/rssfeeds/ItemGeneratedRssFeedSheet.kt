@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.rssfeeds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.TextLabelValue
import java.text.Normalizer

@Composable
fun ItemGeneratedRssFeedSheet(
  sheetState: SheetState,
  title: String,
  rssFeed: GeneratedRssFeedUiState,
  isProcessing: Boolean,
  onDismiss: () -> Unit,
  onCopyUrl: (String) -> Unit,
  onOpenFeed: (String, Boolean, String, String) -> Unit,
  onCloseFeed: (String) -> Unit,
  onShowMessage: (String) -> Unit,
) {
  var showCloseDialog by rememberSaveable { mutableStateOf(false) }
  val currentFeed = rssFeed.currentFeed
  val isCreateMode = currentFeed == null

  var slug by
    rememberSaveable(rssFeed.defaultSlug, currentFeed?.id) {
      mutableStateOf(rssFeed.defaultSlug)
    }
  var preventIndexing by
    rememberSaveable(currentFeed?.id) {
      mutableStateOf(currentFeed?.preventIndexing ?: true)
    }
  var ownerName by
    rememberSaveable(currentFeed?.id) {
      mutableStateOf(currentFeed?.ownerName.orEmpty())
    }
  var ownerEmail by
    rememberSaveable(currentFeed?.id) {
      mutableStateOf(currentFeed?.ownerEmail.orEmpty())
    }

  val previewUrl =
    remember(rssFeed.webBaseUrl, slug) { resolvePreviewFeedUrl(rssFeed.webBaseUrl, slug) }
  val slugRequiredMessage = stringResource(R.string.rss_feed_slug_required)
  val slugSanitizedMessage = stringResource(R.string.rss_feed_slug_sanitized)

  MyAlertDialog(
    showDialog = showCloseDialog,
    title = stringResource(R.string.close_rss_feed),
    text = stringResource(R.string.close_rss_feed_confirm),
    confirmText = stringResource(R.string.close_rss_feed),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      currentFeed?.id?.let(onCloseFeed)
      showCloseDialog = false
    },
    onDismiss = { showCloseDialog = false },
  )

  ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 48.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(text = title, style = MaterialTheme.typography.headlineSmall)
      Text(
        text =
          stringResource(
            if (isCreateMode) R.string.open_generated_rss_feed else R.string.generated_rss_feed
          ),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )

      if (isCreateMode) {
        OutlinedTextField(
          value = slug,
          onValueChange = { slug = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.rss_feed_slug)) },
          singleLine = true,
          enabled = !isProcessing,
        )
        Text(
          text =
            stringResource(
              R.string.generated_rss_feed_url_preview,
              previewUrl,
            ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()
        Text(
          text = stringResource(R.string.rss_details),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        RowCheckbox(
          checked = preventIndexing,
          text = stringResource(R.string.rss_feed_prevent_indexing_directories),
          enabled = !isProcessing,
          onCheckedChange = { preventIndexing = it },
        )

        Text(
          text = stringResource(R.string.advanced),
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        OutlinedTextField(
          value = ownerName,
          onValueChange = { ownerName = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.rss_feed_owner_name)) },
          singleLine = true,
          enabled = !isProcessing,
        )
        OutlinedTextField(
          value = ownerEmail,
          onValueChange = { ownerEmail = it },
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.rss_feed_owner_email)) },
          singleLine = true,
          enabled = !isProcessing,
        )

        if (rssFeed.webBaseUrl.startsWith("http://")) {
          Text(
            text = stringResource(R.string.generated_rss_feed_https_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
        if (rssFeed.hasEpisodesWithoutPubDate) {
          Text(
            text = stringResource(R.string.generated_rss_feed_pub_date_warning),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
          )
        }
      } else {
        val openFeed = requireNotNull(currentFeed)
        OutlinedTextField(
          value = rssFeed.publicFeedUrl,
          onValueChange = {},
          modifier = Modifier.fillMaxWidth(),
          label = { Text(stringResource(R.string.public_feed_url)) },
          readOnly = true,
          singleLine = true,
          trailingIcon = {
            IconButton(onClick = { onCopyUrl(rssFeed.publicFeedUrl) }) {
              Icon(
                painter = painterResource(R.drawable.copy),
                contentDescription = stringResource(R.string.copy_rss_url),
              )
            }
          },
        )

        TextLabelValue(
          label = stringResource(R.string.rss_feed_prevent_indexing),
          value =
            if (openFeed.preventIndexing) stringResource(R.string.yes)
            else stringResource(R.string.no),
          labelWeight = 2f,
        )
        openFeed.ownerName
          ?.takeIf { it.isNotBlank() }
          ?.let { value ->
            TextLabelValue(
              label = stringResource(R.string.rss_feed_owner_name),
              value = value,
              labelWeight = 2f,
            )
          }
        openFeed.ownerEmail
          ?.takeIf { it.isNotBlank() }
          ?.let { value ->
            TextLabelValue(
              label = stringResource(R.string.rss_feed_owner_email),
              value = value,
              labelWeight = 2f,
            )
          }
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (rssFeed.canManage) {
        Button(
          onClick = {
            if (isCreateMode) {
              if (slug.isBlank()) {
                onShowMessage(slugRequiredMessage)
                return@Button
              }
              val sanitized = sanitizeRssFeedSlug(slug)
              if (sanitized != slug) {
                slug = sanitized
                onShowMessage(slugSanitizedMessage)
                return@Button
              }
              onOpenFeed(slug, preventIndexing, ownerName, ownerEmail)
            } else {
              showCloseDialog = true
            }
          },
          enabled = !isProcessing,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = stringResource(if (isCreateMode) R.string.open_feed else R.string.close_rss_feed)
          )
        }
      }
    }
  }
}

@Composable
private fun RowCheckbox(
  checked: Boolean,
  text: String,
  enabled: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  androidx.compose.foundation.layout.Row(modifier = Modifier.fillMaxWidth()) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    Text(
      text = text,
      modifier = Modifier.padding(top = 12.dp),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

internal fun sanitizeRssFeedSlug(value: String): String {
  if (value.isBlank()) return ""

  val normalized =
    Normalizer.normalize(value.trim().lowercase(), Normalizer.Form.NFD)
      .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")

  return normalized
    .replace('.', '-')
    .replace("[^a-z0-9 -_]".toRegex(), "")
    .replace("\\s+".toRegex(), "-")
    .replace("-+".toRegex(), "-")
}

private fun resolvePreviewFeedUrl(webBaseUrl: String, slug: String): String {
  val baseUrl = AudiobookshelfBaseUrl.parse(webBaseUrl) ?: AudiobookshelfBaseUrl.DEFAULT
  val path = if (slug.isBlank()) "/feed/" else "/feed/$slug"
  return baseUrl.resolve(path)
}
