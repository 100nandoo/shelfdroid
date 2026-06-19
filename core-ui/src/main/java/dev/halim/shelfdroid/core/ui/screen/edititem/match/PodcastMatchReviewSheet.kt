@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.match

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchDraft
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchField
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchReviewState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CheckboxRow
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.TextBodyMedium
import dev.halim.shelfdroid.core.ui.components.TextBodySmall
import dev.halim.shelfdroid.core.ui.components.TextTitleLarge
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState

@Composable
fun PodcastMatchReviewSheet(
  currentCoverUrl: String,
  details: DetailsForm,
  review: PodcastMatchReviewState,
  sheetState: SheetState,
  onDismiss: () -> Unit,
  onToggleField: (PodcastMatchField) -> Unit,
  onTitleChange: (String) -> Unit,
  onAuthorChange: (String) -> Unit,
  onFeedUrlChange: (String) -> Unit,
  onItunesIdChange: (String) -> Unit,
  onReleaseDateChange: (String) -> Unit,
  onExplicitChange: (Boolean) -> Unit,
  onApply: () -> Unit,
) {
  ModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismiss) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TextTitleLarge(
        text = stringResource(R.string.edit_item_match_review_title),
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      TextBodyMedium(
        text = review.result.title,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
      )

      CoverReviewRow(
        currentCoverUrl = currentCoverUrl,
        matchedCoverUrl = review.result.cover,
        checked = PodcastMatchField.Cover in review.selectedFields,
        enabled = review.result.cover.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Cover) },
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_title),
        currentValue = details.title,
        editedValue = review.draft.title,
        checked = PodcastMatchField.Title in review.selectedFields,
        enabled = review.result.title.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Title) },
        onValueChange = onTitleChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.author),
        currentValue = details.podcastAuthor,
        editedValue = review.draft.author,
        checked = PodcastMatchField.Author in review.selectedFields,
        enabled = review.result.author.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.Author) },
        onValueChange = onAuthorChange,
      )
      ReadOnlyReviewRow(
        label = stringResource(R.string.genre),
        currentValue = details.genres.joinToString(),
        matchedValue = review.result.genres.joinToString(),
        checked = PodcastMatchField.Genres in review.selectedFields,
        enabled = review.result.genres.isNotEmpty(),
        onCheckedChange = { onToggleField(PodcastMatchField.Genres) },
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_rss_feed_url),
        currentValue = details.rssFeedUrl,
        editedValue = review.draft.feedUrl,
        checked = PodcastMatchField.RssFeedUrl in review.selectedFields,
        enabled = review.result.feedUrl.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.RssFeedUrl) },
        onValueChange = onFeedUrlChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_itunes_id),
        currentValue = details.itunesId,
        editedValue = review.draft.itunesId,
        checked = PodcastMatchField.ItunesId in review.selectedFields,
        enabled = review.result.itunesId.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.ItunesId) },
        onValueChange = onItunesIdChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_release_date),
        currentValue = details.releaseDate,
        editedValue = review.draft.releaseDate,
        checked = PodcastMatchField.ReleaseDate in review.selectedFields,
        enabled = review.result.releaseDate.isNotBlank(),
        onCheckedChange = { onToggleField(PodcastMatchField.ReleaseDate) },
        onValueChange = onReleaseDateChange,
      )
      ExplicitReviewRow(
        currentValue = details.explicit,
        editedValue = review.draft.explicit,
        checked = PodcastMatchField.Explicit in review.selectedFields,
        onCheckedChange = { onToggleField(PodcastMatchField.Explicit) },
        onValueChange = onExplicitChange,
      )

      HorizontalDivider()
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        Button(onClick = onApply, enabled = review.selectedFields.isNotEmpty()) {
          Text(stringResource(R.string.edit_item_apply_selected))
        }
      }
    }
  }
}

@Composable
private fun CoverReviewRow(
  currentCoverUrl: String,
  matchedCoverUrl: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
) {
  Column {
    CheckboxRow(
      checked = checked,
      text = stringResource(R.string.edit_item_cover),
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleMedium,
    )
    Row(modifier = Modifier.padding(start = 48.dp)) {
      CoverPreviewCard(
        title = stringResource(R.string.edit_item_current_cover),
        coverUrl = currentCoverUrl,
        modifier = Modifier.weight(1f),
      )
      CoverPreviewCard(
        title = stringResource(R.string.edit_item_matched_cover),
        coverUrl = matchedCoverUrl,
        modifier = Modifier.weight(1f),
      )
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

@Composable
private fun CoverPreviewCard(title: String, coverUrl: String, modifier: Modifier = Modifier) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    TextBodySmall(
      text = title,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    CoverNoAnimation(
      modifier = Modifier.height(60.dp),
      coverUrl = coverUrl,
      showFallback = true,
    )
  }
}

@Composable
private fun EditableTextReviewRow(
  label: String,
  currentValue: String,
  editedValue: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
  onValueChange: (String) -> Unit,
) {
  Column {
    CheckboxRow(
      checked = checked,
      text = label,
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleMedium,
    )
    OutlinedTextField(
      value = editedValue,
      onValueChange = onValueChange,
      enabled = enabled && checked,
      label = { Text(stringResource(R.string.edit_item_matched_value_label)) },
      modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 16.dp),
      singleLine = true,
      supportingText = {
        TextBodySmall(
          text =
            stringResource(
              R.string.edit_item_current_value,
              currentValue.ifBlank { stringResource(R.string.edit_item_none) },
            ),
          modifier = Modifier.alpha(enabled.enableAlpha()),
        )
      },
    )
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

@Composable
private fun ReadOnlyReviewRow(
  label: String,
  currentValue: String,
  matchedValue: String,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
) {
  Column {
    CheckboxRow(
      checked = checked,
      text = label,
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
    )
    TextBodySmall(
      text =
        stringResource(
          R.string.edit_item_matched_value,
          matchedValue.ifBlank { stringResource(R.string.edit_item_none) },
        ),
      modifier = Modifier.padding(start = 48.dp, end = 16.dp),
    )
    Spacer(Modifier.height(8.dp))
    TextBodySmall(
      text =
        stringResource(
          R.string.edit_item_current_value,
          currentValue.ifBlank { stringResource(R.string.edit_item_none) },
        ),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 48.dp, end = 16.dp),
    )

    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

@Composable
private fun ExplicitReviewRow(
  currentValue: Boolean,
  editedValue: Boolean,
  checked: Boolean,
  onCheckedChange: () -> Unit,
  onValueChange: (Boolean) -> Unit,
) {
  Column {
    CheckboxRow(
      checked = checked,
      text = stringResource(R.string.edit_item_explicit),
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
    )
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clickable(enabled = checked) { onValueChange(!editedValue) }
          .padding(start = 36.dp, end = 16.dp)
          .alpha(checked.enableAlpha()),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(checked = editedValue, onCheckedChange = { onValueChange(it) }, enabled = checked)
      TextBodyMedium(text = stringResource(R.string.edit_item_explicit))
    }
    TextBodySmall(
      text = stringResource(R.string.edit_item_current_value, booleanLabel(currentValue)),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 48.dp, end = 16.dp),
    )
  }
}

@Composable
private fun booleanLabel(value: Boolean): String =
  if (value) stringResource(R.string.yes) else stringResource(R.string.no)

@ShelfDroidPreview
@Composable
private fun PodcastMatchReviewSheetPreview() {
  val density = LocalDensity.current
  val previewReview =
    PodcastMatchReviewState(
      result = Defaults.EDIT_ITEM_PODCAST_MATCH_RESULTS.first(),
      draft =
        PodcastMatchDraft(
          title = "Edited Podcast Title",
          author = "Edited Author",
          feedUrl = "https://edited.example.com/feed.xml",
          itunesId = "99887766",
          releaseDate = "2026-06-19",
          explicit = true,
        ),
      selectedFields =
        setOf(
          PodcastMatchField.Cover,
          PodcastMatchField.Title,
          PodcastMatchField.Author,
          PodcastMatchField.Genres,
          PodcastMatchField.RssFeedUrl,
          PodcastMatchField.ItunesId,
          PodcastMatchField.ReleaseDate,
          PodcastMatchField.Explicit,
        ),
    )
  AnimatedPreviewWrapper {
    PodcastMatchReviewSheet(
      currentCoverUrl = Defaults.BOOK_COVER,
      details = Defaults.EDIT_ITEM_PODCAST_DETAILS_FORM,
      review = previewReview,
      sheetState = sheetState(density),
      onDismiss = {},
      onToggleField = {},
      onTitleChange = {},
      onAuthorChange = {},
      onFeedUrlChange = {},
      onItunesIdChange = {},
      onReleaseDateChange = {},
      onExplicitChange = {},
      onApply = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun EditableTextReviewRowPreview() {
  PreviewWrapper {
    EditableTextReviewRow(
      label = "Title",
      currentValue = "Current Podcast Title",
      editedValue = "Matched Podcast Title",
      checked = true,
      enabled = true,
      onCheckedChange = {},
      onValueChange = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ReadOnlyReviewRowPreview() {
  PreviewWrapper {
    ReadOnlyReviewRow(
      label = "Genres",
      currentValue = "Technology",
      matchedValue = "Technology, Software",
      checked = true,
      enabled = true,
      onCheckedChange = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ExplicitReviewRowPreview() {
  PreviewWrapper {
    ExplicitReviewRow(
      currentValue = false,
      editedValue = true,
      checked = true,
      onCheckedChange = {},
      onValueChange = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun CoverReviewRowPreview() {
  PreviewWrapper {
    CoverReviewRow(
      currentCoverUrl = Defaults.BOOK_COVER,
      matchedCoverUrl = Defaults.BOOK_COVER,
      checked = true,
      enabled = true,
      onCheckedChange = {},
    )
  }
}
