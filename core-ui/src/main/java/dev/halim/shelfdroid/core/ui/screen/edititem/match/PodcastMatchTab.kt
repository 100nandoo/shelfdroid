@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.MatchState
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchField
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchResultRow
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchReviewState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun PodcastMatchTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val match = uiState.match as? MatchState.Podcast ?: return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText =
      match.providers.find { it.value == match.selectedProvider }?.text ?: match.selectedProvider

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
      OutlinedTextField(
        value = selectedText,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.edit_item_provider)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier =
          Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      )
      ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        match.providers.forEach { provider ->
          DropdownMenuItem(
            text = { Text(provider.text) },
            onClick = {
              onEvent(EditItemEvent.UpdateMatchProvider(provider.value))
              expanded = false
            },
          )
        }
      }
    }

    OutlinedTextField(
      value = match.searchTerm,
      onValueChange = { onEvent(EditItemEvent.UpdatePodcastMatchSearchTerm(it)) },
      label = { Text(stringResource(R.string.edit_item_match_search_term)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Button(
      onClick = { onEvent(EditItemEvent.RunMatchSearch) },
      enabled = !match.isSearching && match.searchTerm.isNotBlank(),
    ) {
      Text(stringResource(R.string.search))
    }

    if (match.isSearching) {
      CircularProgressIndicator()
    }

    match.results.forEachIndexed { index, result ->
      Card(
        modifier =
          Modifier.fillMaxWidth().clickable { onEvent(EditItemEvent.OpenPodcastMatchReview(index)) }
      ) {
        Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (result.cover.isNotBlank()) {
            CoverNoAnimation(modifier = Modifier.size(64.dp), coverUrl = result.cover)
          }
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              text = result.title,
              style = MaterialTheme.typography.titleSmall,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
            )
            if (result.author.isNotBlank()) {
              Text(
                text = stringResource(R.string.edit_item_match_by_author, result.author),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Text(
              text =
                stringResource(
                  R.string.edit_item_podcast_match_summary,
                  result.genres.joinToString().ifBlank { stringResource(R.string.edit_item_none) },
                  result.episodeCount,
                ),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (result.description.isNotBlank()) {
              Text(
                text = result.description,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }

  val review = match.review
  if (review != null) {
    PodcastMatchReviewSheet(
      details = uiState.details,
      review = review,
      sheetState = sheetState,
      onDismiss = { onEvent(EditItemEvent.DismissPodcastMatchReview) },
      onToggleField = { onEvent(EditItemEvent.TogglePodcastMatchField(it)) },
      onApply = { onEvent(EditItemEvent.ApplyPodcastMatchReview) },
    )
  }
}

@Composable
private fun PodcastMatchReviewSheet(
  details: DetailsForm,
  review: PodcastMatchReviewState,
  sheetState: androidx.compose.material3.SheetState,
  onDismiss: () -> Unit,
  onToggleField: (PodcastMatchField) -> Unit,
  onApply: () -> Unit,
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = onDismiss,
  ) {
    Column(
      modifier =
        Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = stringResource(R.string.edit_item_match_review_title),
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        text = review.result.title,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      PodcastReviewRow(
        label = stringResource(R.string.edit_item_cover),
        currentValue = stringResource(R.string.edit_item_cover_preview_current),
        matchedValue = review.result.cover.ifBlank { stringResource(R.string.edit_item_none) },
        checked = PodcastMatchField.Cover in review.selectedFields,
        enabled = review.result.cover.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Cover) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.edit_item_title),
        currentValue = details.title,
        matchedValue = review.result.title,
        checked = PodcastMatchField.Title in review.selectedFields,
        enabled = review.result.title.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Title) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.author),
        currentValue = details.podcastAuthor,
        matchedValue = review.result.author,
        checked = PodcastMatchField.Author in review.selectedFields,
        enabled = review.result.author.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Author) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.genre),
        currentValue = details.genres.joinToString(),
        matchedValue = review.result.genres.joinToString(),
        checked = PodcastMatchField.Genres in review.selectedFields,
        enabled = review.result.genres.isNotEmpty(),
        onCheckedChange = { onToggleField(PodcastMatchField.Genres) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.edit_item_rss_feed_url),
        currentValue = details.rssFeedUrl,
        matchedValue = review.result.feedUrl,
        checked = PodcastMatchField.RssFeedUrl in review.selectedFields,
        enabled = review.result.feedUrl.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.RssFeedUrl) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.edit_item_itunes_id),
        currentValue = details.itunesId,
        matchedValue = review.result.itunesId,
        checked = PodcastMatchField.ItunesId in review.selectedFields,
        enabled = review.result.itunesId.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.ItunesId) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.edit_item_release_date),
        currentValue = details.releaseDate,
        matchedValue = review.result.releaseDate,
        checked = PodcastMatchField.ReleaseDate in review.selectedFields,
        enabled = review.result.releaseDate.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.ReleaseDate) },
      )
      PodcastReviewRow(
        label = stringResource(R.string.edit_item_explicit),
        currentValue = booleanLabel(details.explicit),
        matchedValue = booleanLabel(review.result.explicit),
        checked = PodcastMatchField.Explicit in review.selectedFields,
        enabled = true,
        onCheckedChange = { onToggleField(PodcastMatchField.Explicit) },
      )

      HorizontalDivider()
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        Button(
          onClick = onApply,
          enabled = review.selectedFields.isNotEmpty(),
        ) {
          Text(stringResource(R.string.edit_item_apply_selected))
        }
      }
    }
  }
}

@Composable
private fun PodcastReviewRow(
  label: String,
  currentValue: String,
  matchedValue: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Checkbox(checked = checked, onCheckedChange = { onCheckedChange() }, enabled = enabled)
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
      Text(
        text = stringResource(R.string.edit_item_current_value, currentValue.ifBlank { stringResource(R.string.edit_item_none) }),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text = stringResource(R.string.edit_item_matched_value, matchedValue.ifBlank { stringResource(R.string.edit_item_none) }),
        style = MaterialTheme.typography.bodySmall,
      )
    }
  }
}

@Composable
private fun booleanLabel(value: Boolean): String =
  if (value) stringResource(R.string.yes) else stringResource(R.string.no)

@ShelfDroidPreview
@Composable
private fun PodcastMatchTabPreview() {
  PreviewWrapper { PodcastMatchTab(uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE, onEvent = {}) }
}
