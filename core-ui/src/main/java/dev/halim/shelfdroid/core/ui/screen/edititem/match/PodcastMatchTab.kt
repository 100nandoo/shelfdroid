@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.match

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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.MatchState
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchDraft
import dev.halim.shelfdroid.core.data.screen.edititem.PodcastMatchResultRow
import dev.halim.shelfdroid.core.data.screen.edititem.displayCoverUrl
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun PodcastMatchTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val match = uiState.match as? MatchState.Podcast ?: return
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  fun updateMatch(transform: (MatchState.Podcast) -> MatchState.Podcast) {
    onEvent(EditItemEvent.UpdatePodcastMatch(transform))
  }

  fun updateDraft(transform: (PodcastMatchDraft) -> PodcastMatchDraft) {
    onEvent(EditItemEvent.UpdatePodcastMatchDraft(transform))
  }

  LazyColumn(
    reverseLayout = true,
    verticalArrangement = Arrangement.Bottom,
    modifier = Modifier.fillMaxSize(),
  ) {
    item {
      PodcastMatchSearchControls(
        modifier = Modifier.animateItem(),
        match = match,
        onUpdateMatch = ::updateMatch,
        onRunSearch = { onEvent(EditItemEvent.RunMatchSearch) },
      )
    }

    itemsIndexed(
      items = match.results,
      key = { index, result -> result.itunesId.ifBlank { "${result.title}-$index" } },
    ) { index, result ->
      PodcastMatchResultListRow(
        modifier = Modifier.animateItem(),
        result = result,
        onClick = { onEvent(EditItemEvent.OpenPodcastMatchReview(index)) },
      )
    }
  }

  val review = match.review
  if (review != null) {
    PodcastMatchReviewSheet(
      currentCoverUrl = uiState.displayCoverUrl(),
      details = uiState.details,
      review = review,
      isApplying = uiState.isSaving,
      sheetState = sheetState,
      onDismiss = { onEvent(EditItemEvent.DismissPodcastMatchReview) },
      onToggleField = { onEvent(EditItemEvent.TogglePodcastMatchField(it)) },
      onTitleChange = { value -> updateDraft { it.copy(title = value) } },
      onAuthorChange = { value -> updateDraft { it.copy(author = value) } },
      onFeedUrlChange = { value -> updateDraft { it.copy(feedUrl = value) } },
      onItunesIdChange = { value -> updateDraft { it.copy(itunesId = value) } },
      onReleaseDateChange = { value -> updateDraft { it.copy(releaseDate = value) } },
      onExplicitChange = { value -> updateDraft { it.copy(explicit = value) } },
      onApply = { onEvent(EditItemEvent.ApplyPodcastMatchReview) },
    )
  }
}

@Composable
private fun PodcastMatchSearchControls(
  modifier: Modifier = Modifier,
  match: MatchState.Podcast,
  onUpdateMatch: ((MatchState.Podcast) -> MatchState.Podcast) -> Unit,
  onRunSearch: () -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedText =
    match.providers.find { it.value == match.selectedProvider }?.text ?: match.selectedProvider

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
              onUpdateMatch { it.copy(selectedProvider = provider.value) }
              expanded = false
            },
          )
        }
      }
    }

    OutlinedTextField(
      value = match.searchTerm,
      onValueChange = { onUpdateMatch { matchState -> matchState.copy(searchTerm = it) } },
      label = { Text(stringResource(R.string.edit_item_match_search_term)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    Button(
      onClick = onRunSearch,
      enabled = !match.isSearching && match.searchTerm.isNotBlank(),
      modifier = Modifier.align(Alignment.End),
    ) {
      Text(stringResource(R.string.search))
    }
  }
}

@Composable
private fun PodcastMatchResultListRow(
  modifier: Modifier = Modifier,
  result: PodcastMatchResultRow,
  onClick: () -> Unit,
) {
  Column(modifier = modifier.fillMaxWidth().clickable(onClick = onClick)) {
    Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
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
        Text(
          text =
            stringResource(
              R.string.edit_item_podcast_match_summary,
              result.genres.joinToString().ifBlank { stringResource(R.string.edit_item_none) },
              result.episodeCount,
            ),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
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
    HorizontalDivider()
  }
}

@ShelfDroidPreview
@Composable
private fun PodcastMatchTabPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    PodcastMatchTab(
      uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE,
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun PodcastMatchTabEmptyPreview() {
  AnimatedPreviewWrapper {
    PodcastMatchTab(
      uiState =
        Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(
          match =
            (Defaults.EDIT_ITEM_PODCAST_UI_STATE.match as MatchState.Podcast).copy(
              hasSearched = true,
              results = emptyList(),
            )
        ),
      onEvent = {},
    )
  }
}
