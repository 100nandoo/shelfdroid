package dev.halim.shelfdroid.core.ui.screen.editepisode.tabs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.editepisode.EpisodeMatchResultRow
import dev.halim.shelfdroid.core.data.screen.editepisode.EpisodeMatchState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.editepisode.EditEpisodeEvent
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun EditEpisodeMatchTab(
  match: EpisodeMatchState,
  isApplying: Boolean,
  onEvent: (EditEpisodeEvent) -> Unit,
) {
  val searchEnabled = match.searchTerm.isNotBlank() && !match.isSearching && !isApplying
  val runSearch = {
    if (searchEnabled) {
      onEvent(EditEpisodeEvent.RunMatchSearch)
    }
  }

  Column(modifier = Modifier.fillMaxSize()) {
    AnimatedVisibility(visible = match.isSearching) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    when {
      match.isSearching && match.results.isEmpty() ->
        Box(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }

      match.errorMessage != null -> {
        val errorMessage = match.errorMessage.orEmpty()
        EpisodeMatchMessage(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          text = errorMessage,
          color = MaterialTheme.colorScheme.error,
        )
      }

      match.hasSearched && match.results.isEmpty() ->
        EpisodeMatchMessage(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          text = stringResource(R.string.edit_item_match_no_results),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

      else ->
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          reverseLayout = true,
          verticalArrangement = Arrangement.Bottom,
        ) {
          itemsIndexed(
            items = match.results,
            key = { index, result -> "${result.enclosureUrl}-${result.title}-$index" },
          ) { index, result ->
            EpisodeMatchResultListRow(
              result = result,
              enabled = !isApplying,
              onClick = { onEvent(EditEpisodeEvent.ApplyMatch(index)) },
            )
          }
        }
    }

    OutlinedTextField(
      value = match.searchTerm,
      onValueChange = { value ->
        onEvent(EditEpisodeEvent.UpdateMatch { it.copy(searchTerm = value) })
      },
      label = { Text(stringResource(R.string.edit_item_match_search_term)) },
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
      keyboardActions = KeyboardActions(onSearch = { runSearch() }),
      trailingIcon = {
        IconButton(onClick = runSearch, enabled = searchEnabled) {
          Icon(
            painter = painterResource(id = R.drawable.search),
            contentDescription = stringResource(R.string.search),
          )
        }
      },
      singleLine = true,
    )
  }
}

@Composable
private fun EpisodeMatchMessage(
  modifier: Modifier = Modifier,
  text: String,
  color: Color,
) {
  Box(
    modifier = modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(text = text, color = color, style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun EpisodeMatchResultListRow(
  result: EpisodeMatchResultRow,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Column(modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick)) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = result.title,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )

      if (result.subtitle.isNotBlank()) {
        Text(
          text = result.subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      val secondaryText = resultSecondaryText(result)
      if (secondaryText.isNotBlank()) {
        Text(
          text = secondaryText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
    HorizontalDivider()
  }
}

@Composable
private fun resultSecondaryText(result: EpisodeMatchResultRow): String {
  val episodeLabel = stringResource(R.string.edit_episode_number)
  val publishedAt =
    result.publishedAtMillis?.let {
      SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it))
    }
  return listOfNotNull(
      result.episode.takeIf { it.isNotBlank() }?.let { "$episodeLabel $it" },
      publishedAt,
    )
    .joinToString(" • ")
}

@ShelfDroidPreview
@Composable
private fun EditEpisodeMatchTabPreview() {
  PreviewWrapper {
    EditEpisodeMatchTab(
      match =
        EpisodeMatchState(
          searchTerm = "Daily Pod episode 12",
          results =
            listOf(
              EpisodeMatchResultRow(
                episode = "12",
                title = "Episode 12: The Rewrite",
                subtitle = "Why this one took longer than planned",
                enclosureUrl = "https://example.com/episode-12.mp3",
                publishedAtMillis = 1781818200000L,
              ),
              EpisodeMatchResultRow(
                episode = "11",
                title = "Episode 11: Shipping the Fix",
                subtitle = "A shorter recap with release notes",
                enclosureUrl = "https://example.com/episode-11.mp3",
                publishedAtMillis = 1781213400000L,
              ),
            ),
          hasSearched = true,
        ),
      isApplying = false,
      onEvent = {},
    )
  }
}
