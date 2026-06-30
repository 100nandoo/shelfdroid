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
import dev.halim.shelfdroid.core.data.screen.edititem.BookMatchDraft
import dev.halim.shelfdroid.core.data.screen.edititem.BookMatchField
import dev.halim.shelfdroid.core.data.screen.edititem.BookMatchReviewResult
import dev.halim.shelfdroid.core.data.screen.edititem.BookMatchReviewState
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.SeriesEntry
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
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.ChipInput
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.SeriesInput

@Composable
fun BookMatchReviewSheet(
  currentCoverUrl: String,
  details: DetailsForm,
  review: BookMatchReviewState,
  isApplying: Boolean,
  sheetState: SheetState,
  seriesSuggestions: List<String>,
  onDismiss: () -> Unit,
  onToggleField: (BookMatchField) -> Unit,
  onTitleChange: (String) -> Unit,
  onSubtitleChange: (String) -> Unit,
  onAuthorsChange: (List<String>) -> Unit,
  onNarratorsChange: (List<String>) -> Unit,
  onPublisherChange: (String) -> Unit,
  onPublishedYearChange: (String) -> Unit,
  onDescriptionChange: (String) -> Unit,
  onIsbnChange: (String) -> Unit,
  onAsinChange: (String) -> Unit,
  onAbridgedChange: (Boolean) -> Unit,
  onGenresChange: (List<String>) -> Unit,
  onTagsChange: (List<String>) -> Unit,
  onSeriesChange: (List<SeriesEntry>) -> Unit,
  onApply: () -> Unit,
) {
  ModalBottomSheet(
    sheetState = sheetState,
    onDismissRequest = {
      if (!isApplying) onDismiss()
    },
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      TextTitleLarge(
        text = stringResource(R.string.edit_item_match_review_title),
        modifier = Modifier.padding(horizontal = 16.dp),
      )
      TextBodyMedium(
        text = review.result.title.orEmpty().ifBlank { stringResource(R.string.edit_item_none) },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
      )

      CoverReviewRow(
        currentCoverUrl = currentCoverUrl,
        matchedCoverUrl = review.result.cover,
        checked = BookMatchField.Cover in review.selectedFields,
        enabled = review.result.cover.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Cover) },
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_title),
        currentValue = details.title,
        editedValue = review.draft.title,
        checked = BookMatchField.Title in review.selectedFields,
        enabled = review.result.title.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Title) },
        onValueChange = onTitleChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_subtitle),
        currentValue = details.subtitle,
        editedValue = review.draft.subtitle,
        checked = BookMatchField.Subtitle in review.selectedFields,
        enabled = review.result.subtitle.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Subtitle) },
        onValueChange = onSubtitleChange,
      )
      ChipReviewRow(
        label = stringResource(R.string.edit_item_authors),
        currentValue = details.authors,
        editedValue = review.draft.authors,
        checked = BookMatchField.Authors in review.selectedFields,
        enabled = review.result.authors.isNotEmpty() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Authors) },
        onValueChange = onAuthorsChange,
      )
      ChipReviewRow(
        label = stringResource(R.string.edit_item_narrators),
        currentValue = details.narrators,
        editedValue = review.draft.narrators,
        checked = BookMatchField.Narrators in review.selectedFields,
        enabled = review.result.narrators.isNotEmpty() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Narrators) },
        onValueChange = onNarratorsChange,
      )
      SeriesReviewRow(
        label = stringResource(R.string.edit_item_series),
        currentValue = details.series,
        editedValue = review.draft.series,
        seriesSuggestions = seriesSuggestions,
        checked = BookMatchField.Series in review.selectedFields,
        enabled = review.result.series.isNotEmpty() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Series) },
        onValueChange = onSeriesChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_publish_year),
        currentValue = details.publishedYear,
        editedValue = review.draft.publishedYear,
        checked = BookMatchField.PublishedYear in review.selectedFields,
        enabled = review.result.publishedYear.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.PublishedYear) },
        onValueChange = onPublishedYearChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.description),
        currentValue = details.description,
        editedValue = review.draft.description,
        checked = BookMatchField.Description in review.selectedFields,
        enabled = review.result.description.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Description) },
        onValueChange = onDescriptionChange,
        singleLine = false,
        minLines = 4,
        maxLines = 8,
      )
      ChipReviewRow(
        label = stringResource(R.string.genres),
        currentValue = details.genres,
        editedValue = review.draft.genres,
        checked = BookMatchField.Genres in review.selectedFields,
        enabled = review.result.genres.isNotEmpty() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Genres) },
        onValueChange = onGenresChange,
      )
      ChipReviewRow(
        label = stringResource(R.string.edit_item_tags),
        currentValue = details.tags,
        editedValue = review.draft.tags,
        checked = BookMatchField.Tags in review.selectedFields,
        enabled = review.result.tags.isNotEmpty() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Tags) },
        onValueChange = onTagsChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_isbn),
        currentValue = details.isbn,
        editedValue = review.draft.isbn,
        checked = BookMatchField.Isbn in review.selectedFields,
        enabled = review.result.isbn.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Isbn) },
        onValueChange = onIsbnChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.edit_item_asin),
        currentValue = details.asin,
        editedValue = review.draft.asin,
        checked = BookMatchField.Asin in review.selectedFields,
        enabled = review.result.asin.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Asin) },
        onValueChange = onAsinChange,
      )
      AbridgedReviewRow(
        currentValue = details.abridged,
        editedValue = review.draft.abridged,
        checked = BookMatchField.Abridged in review.selectedFields,
        enabled = review.result.abridged != null && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Abridged) },
        onValueChange = onAbridgedChange,
      )
      EditableTextReviewRow(
        label = stringResource(R.string.publisher),
        currentValue = details.publisher,
        editedValue = review.draft.publisher,
        checked = BookMatchField.Publisher in review.selectedFields,
        enabled = review.result.publisher.isNotBlank() && !isApplying,
        onCheckedChange = { onToggleField(BookMatchField.Publisher) },
        onValueChange = onPublisherChange,
      )

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onDismiss, enabled = !isApplying) {
          Text(stringResource(R.string.cancel))
        }
        Button(onClick = onApply, enabled = !isApplying && review.selectedFields.isNotEmpty()) {
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
  singleLine: Boolean = true,
  minLines: Int = 1,
  maxLines: Int = 1,
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
    OutlinedTextField(
      value = editedValue,
      onValueChange = onValueChange,
      enabled = enabled && checked,
      label = { Text(stringResource(R.string.edit_item_matched_value_label)) },
      modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 16.dp),
      singleLine = singleLine,
      minLines = minLines,
      maxLines = maxLines,
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
private fun ChipReviewRow(
  label: String,
  currentValue: List<String>,
  editedValue: List<String>,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
  onValueChange: (List<String>) -> Unit,
) {
  val fieldEnabled = enabled && checked
  Column {
    CheckboxRow(
      checked = checked,
      text = label,
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
    )
    Column(
      modifier = Modifier.padding(start = 48.dp, end = 16.dp).alpha(fieldEnabled.enableAlpha())
    ) {
      ChipInput(
        label = label,
        values = editedValue,
        onAdd = { value ->
          if (value !in editedValue) onValueChange(editedValue + value)
        },
        onRemove = { value -> onValueChange(editedValue - value) },
        enabled = fieldEnabled,
      )
      Spacer(Modifier.height(8.dp))
      TextBodySmall(
        text =
          stringResource(
            R.string.edit_item_current_value,
            currentValue.joinToString().ifBlank { stringResource(R.string.edit_item_none) },
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

@Composable
private fun SeriesReviewRow(
  label: String,
  currentValue: List<SeriesEntry>,
  editedValue: List<SeriesEntry>,
  seriesSuggestions: List<String>,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
  onValueChange: (List<SeriesEntry>) -> Unit,
) {
  val fieldEnabled = enabled && checked
  Column {
    CheckboxRow(
      checked = checked,
      text = label,
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
    )
    Column(
      modifier = Modifier.padding(start = 48.dp, end = 16.dp).alpha(fieldEnabled.enableAlpha())
    ) {
      SeriesInput(
        label = label,
        values = editedValue,
        suggestions = seriesSuggestions,
        onAdd = { value -> onValueChange(editedValue + value) },
        onRemove = { value -> onValueChange(editedValue - value) },
        enabled = fieldEnabled,
      )
      Spacer(Modifier.height(8.dp))
      TextBodySmall(
        text =
          stringResource(
            R.string.edit_item_current_value,
            currentValue.asSeriesText().ifBlank { stringResource(R.string.edit_item_none) },
          ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

@Composable
private fun AbridgedReviewRow(
  currentValue: Boolean,
  editedValue: Boolean,
  checked: Boolean,
  enabled: Boolean,
  onCheckedChange: () -> Unit,
  onValueChange: (Boolean) -> Unit,
) {
  Column {
    CheckboxRow(
      checked = checked,
      text = stringResource(R.string.edit_item_abridged),
      enabled = enabled,
      onCheckedChange = { onCheckedChange() },
      modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onCheckedChange),
      textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
    )
    Row(
      modifier =
        Modifier.fillMaxWidth()
          .clickable(enabled = enabled && checked) { onValueChange(!editedValue) }
          .padding(start = 36.dp, end = 16.dp)
          .alpha((enabled && checked).enableAlpha()),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Checkbox(
        checked = editedValue,
        onCheckedChange = { onValueChange(it) },
        enabled = enabled && checked,
      )
      TextBodyMedium(text = stringResource(R.string.edit_item_abridged))
    }
    TextBodySmall(
      text = stringResource(R.string.edit_item_current_value, booleanLabel(currentValue)),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(start = 48.dp, end = 16.dp),
    )
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
  }
}

private fun List<SeriesEntry>.asSeriesText(): String = joinToString { entry ->
  if (entry.sequence.isNotBlank()) "${entry.name} #${entry.sequence}" else entry.name
}

@Composable
private fun booleanLabel(value: Boolean): String =
  if (value) stringResource(R.string.yes) else stringResource(R.string.no)

@ShelfDroidPreview
@Composable
private fun BookMatchReviewSheetPreview() {
  val density = LocalDensity.current
  val previewReview =
    BookMatchReviewState(
      result =
        BookMatchReviewResult(
          title = "Signal in the Harbor",
          subtitle = "Harbor Cycle, Book 1",
          authors = listOf("Iris Vale", "Rowan Chase"),
          narrators = listOf("Noah Park"),
          publisher = "North Pier Audio",
          publishedYear = "2026",
          description = "Matched book description.",
          cover = Defaults.BOOK_COVER,
          isbn = "9780000000000",
          asin = "B000000000",
          abridged = false,
          genres = listOf("Adventure", "Mystery"),
          tags = listOf("featured", "coastal"),
          series = listOf(SeriesEntry("Harbor Cycle", "1")),
        ),
      draft =
        BookMatchDraft(
          title = "Edited Signal in the Harbor",
          subtitle = "Edited subtitle",
          authors = listOf("Iris Vale", "Rowan Chase"),
          narrators = listOf("Noah Park"),
          publisher = "Edited Publisher",
          publishedYear = "2026",
          description = "Edited description.",
          isbn = "9781111111111",
          asin = "B111111111",
          abridged = true,
          genres = listOf("Adventure"),
          tags = listOf("featured"),
          series = listOf(SeriesEntry("Harbor Cycle", "1")),
        ),
      selectedFields = BookMatchField.entries.toSet(),
    )
  AnimatedPreviewWrapper {
    BookMatchReviewSheet(
      currentCoverUrl = Defaults.BOOK_COVER,
      details = Defaults.EDIT_ITEM_DETAILS_FORM,
      review = previewReview,
      isApplying = false,
      sheetState = sheetState(density),
      seriesSuggestions = listOf("Harbor Cycle", "Lantern Files"),
      onDismiss = {},
      onToggleField = {},
      onTitleChange = {},
      onSubtitleChange = {},
      onAuthorsChange = {},
      onNarratorsChange = {},
      onPublisherChange = {},
      onPublishedYearChange = {},
      onDescriptionChange = {},
      onIsbnChange = {},
      onAsinChange = {},
      onAbridgedChange = {},
      onGenresChange = {},
      onTagsChange = {},
      onSeriesChange = {},
      onApply = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun ChipReviewRowPreview() {
  PreviewWrapper {
    ChipReviewRow(
      label = "Authors",
      currentValue = listOf("Current Author"),
      editedValue = listOf("Matched Author"),
      checked = true,
      enabled = true,
      onCheckedChange = {},
      onValueChange = {},
    )
  }
}
