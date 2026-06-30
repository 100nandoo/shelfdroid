@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.match

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.BookMatchDraft
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.MatchResultRow
import dev.halim.shelfdroid.core.data.screen.edititem.MatchState
import dev.halim.shelfdroid.core.data.screen.edititem.displayCoverUrl
import dev.halim.shelfdroid.core.data.screen.edititem.initialBookMatchSearch
import dev.halim.shelfdroid.core.data.screen.edititem.isAudibleMatchProvider
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.TextBodyLarge
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun BookMatchTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val match = uiState.match as? MatchState.Book ?: return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  fun updateMatch(transform: (MatchState.Book) -> MatchState.Book) {
    onEvent(EditItemEvent.UpdateBookMatch(transform))
  }

  fun updateDraft(transform: (BookMatchDraft) -> BookMatchDraft) {
    onEvent(EditItemEvent.UpdateBookMatchDraft(transform))
  }

  LazyColumn(
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      BookMatchSearchControls(
        modifier = Modifier.animateItem(),
        details = uiState.details,
        match = match,
        onUpdateMatch = ::updateMatch,
        onRunSearch = { onEvent(EditItemEvent.RunMatchSearch) },
      )
    }

    if (match.hasSearched && match.results.isEmpty() && !match.isSearching) {
      item {
        BookMatchEmptyState(modifier = Modifier.animateItem())
      }
    }

    itemsIndexed(
      items = match.results,
      key = { index, result -> "${result.title}-${result.author}-$index" },
    ) { index, result ->
      BookMatchResultListRow(
        modifier = Modifier.animateItem(),
        result = result,
        onClick = { onEvent(EditItemEvent.OpenBookMatchReview(index)) },
      )
    }
  }

  val review = match.review
  if (review != null) {
    BookMatchReviewSheet(
      currentCoverUrl = uiState.displayCoverUrl(),
      details = uiState.details,
      review = review,
      isApplying = uiState.isSaving,
      sheetState = sheetState,
      seriesSuggestions = uiState.seriesSuggestions,
      onDismiss = { onEvent(EditItemEvent.DismissBookMatchReview) },
      onToggleField = { onEvent(EditItemEvent.ToggleBookMatchField(it)) },
      onTitleChange = { value -> updateDraft { it.copy(title = value) } },
      onSubtitleChange = { value -> updateDraft { it.copy(subtitle = value) } },
      onAuthorsChange = { value -> updateDraft { it.copy(authors = value) } },
      onNarratorsChange = { value -> updateDraft { it.copy(narrators = value) } },
      onPublisherChange = { value -> updateDraft { it.copy(publisher = value) } },
      onPublishedYearChange = { value -> updateDraft { it.copy(publishedYear = value) } },
      onDescriptionChange = { value -> updateDraft { it.copy(description = value) } },
      onIsbnChange = { value -> updateDraft { it.copy(isbn = value) } },
      onAsinChange = { value -> updateDraft { it.copy(asin = value) } },
      onAbridgedChange = { value -> updateDraft { it.copy(abridged = value) } },
      onGenresChange = { value -> updateDraft { it.copy(genres = value) } },
      onTagsChange = { value -> updateDraft { it.copy(tags = value) } },
      onSeriesChange = { value -> updateDraft { it.copy(series = value) } },
      onApply = { onEvent(EditItemEvent.ApplyBookMatchReview) },
    )
  }
}

@Composable
private fun BookMatchSearchControls(
  modifier: Modifier = Modifier,
  details: DetailsForm,
  match: MatchState.Book,
  onUpdateMatch: ((MatchState.Book) -> MatchState.Book) -> Unit,
  onRunSearch: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedText =
    match.providers.find { it.value == match.selectedProvider }?.text ?: match.selectedProvider
  val titleLabel =
    if (isAudibleMatchProvider(match.selectedProvider)) {
      R.string.edit_item_search_title_or_asin
    } else {
      R.string.edit_item_search_title
    }

  Column(
    modifier = modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
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
              val searchInput = initialBookMatchSearch(details, provider.value)
              onUpdateMatch {
                it.copy(
                  selectedProvider = provider.value,
                  title = searchInput.title,
                  author = searchInput.author,
                )
              }
              expanded = false
            },
          )
        }
      }
    }

    OutlinedTextField(
      value = match.title,
      onValueChange = { onUpdateMatch { matchState -> matchState.copy(title = it) } },
      label = {
        AnimatedContent(targetState = titleLabel, label = "bookMatchSearchTitleLabel") { label ->
          Text(stringResource(label))
        }
      },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = match.author,
      onValueChange = { onUpdateMatch { matchState -> matchState.copy(author = it) } },
      label = { Text(stringResource(R.string.author)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    Button(
      onClick = onRunSearch,
      enabled = !match.isSearching && match.title.isNotBlank(),
      modifier = Modifier.align(Alignment.End),
    ) {
      Text(stringResource(R.string.search))
    }
  }
}

@Composable
private fun BookMatchEmptyState(modifier: Modifier = Modifier) {
  TextBodyLarge(
    text = stringResource(R.string.edit_item_match_no_results),
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun BookMatchResultListRow(
  modifier: Modifier = Modifier,
  result: MatchResultRow,
  onClick: () -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (result.cover.isNotBlank()) {
        CoverNoAnimation(
          modifier = Modifier.height(60.dp).padding(end = 16.dp),
          coverUrl = result.cover,
          shape = RoundedCornerShape(4.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = result.title,
          style = MaterialTheme.typography.bodyLarge,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        if (result.author.isNotBlank()) {
          Text(
            text = stringResource(R.string.edit_item_match_by_author, result.author),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        if (result.description.isNotBlank()) {
          val description =
            remember(result.description) { AnnotatedString.fromHtml(result.description) }
          Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    }
    HorizontalDivider()
  }
}

@ShelfDroidPreview
@Composable
private fun BookMatchTabPreview() {
  val previewMatch = Defaults.EDIT_ITEM_UI_STATE.match as MatchState.Book
  PreviewWrapper {
    BookMatchTab(
      uiState =
        Defaults.EDIT_ITEM_UI_STATE.copy(
          match =
            previewMatch.copy(
              results =
                previewMatch.results.mapIndexed { index, result ->
                  if (index == 0) result.copy(cover = Defaults.BOOK_COVER) else result
                } +
                  MatchResultRow(
                    cover = Defaults.BOOK_COVER,
                    title =
                      "The Bomber Mafia: A Dream, a Temptation, and the Longest Night of the Second World War",
                    author = "Malcolm Gladwell",
                    description = "A long-title preview result for layout coverage.",
                  )
            )
        ),
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun BookMatchTabEmptyPreview() {
  PreviewWrapper {
    BookMatchTab(
      uiState =
        Defaults.EDIT_ITEM_UI_STATE.copy(
          match =
            (Defaults.EDIT_ITEM_UI_STATE.match as MatchState.Book).copy(
              hasSearched = true,
              results = emptyList(),
            )
        ),
      onEvent = {},
    )
  }
}
