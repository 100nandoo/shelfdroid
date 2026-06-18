package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.data.screen.edititem.EpisodeRow
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DateTimePickerTextField
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun EpisodesTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit = {}) {
  if (uiState.episodes.isEmpty()) {
    LazyColumn(reverseLayout = true) {
      item { EpisodeUpdateControls(uiState = uiState, onEvent = onEvent) }
      item {
        Box(
          modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = stringResource(R.string.edit_item_no_episodes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
    return
  }

  LazyColumn(reverseLayout = true) {
    item { EpisodeUpdateControls(uiState = uiState, onEvent = onEvent) }
    items(uiState.episodes, key = { it.id }) { episode -> EpisodeListRow(episode = episode) }
  }
}

@Composable
private fun EpisodeUpdateControls(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val episodeUpdate = uiState.episodeUpdate

  Column(
    modifier = Modifier.fillMaxWidth().padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    DateTimePickerTextField(
      enabled = !episodeUpdate.isRunning,
      modifier = Modifier.fillMaxWidth(),
      label = stringResource(R.string.edit_item_episode_update_cutoff),
      selectedDateTimeMillis = episodeUpdate.selectedCutoffMillis,
      onDateTimeSelected = { onEvent(EditItemEvent.UpdateEpisodeCutoffMillis(it)) },
      placeholder = stringResource(R.string.edit_item_episode_update_cutoff_hint),
    )
    MyOutlinedTextField(
      enabled = !episodeUpdate.isRunning,
      value = episodeUpdate.limitInput,
      onValueChange = { onEvent(EditItemEvent.UpdateEpisodeLimitInput(it)) },
      label = stringResource(R.string.edit_item_episode_limit),
      placeholder = stringResource(R.string.edit_item_episode_limit_hint),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
    )
    Button(
      enabled = !episodeUpdate.isRunning,
      onClick = { onEvent(EditItemEvent.RunEpisodeUpdateCheck) },
      modifier = Modifier.align(Alignment.End),
    ) {
      Text(
        if (episodeUpdate.isRunning) {
          stringResource(R.string.edit_item_checking_episodes)
        } else {
          stringResource(R.string.edit_item_check_download_new_episodes)
        }
      )
    }
  }
}

@Composable
private fun EpisodeListRow(episode: EpisodeRow) {
  HorizontalDivider()
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = episode.title,
      style = MaterialTheme.typography.bodyLarge,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    if (episode.secondaryText.isNotBlank()) {
      Text(
        text = episode.secondaryText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
private fun EpisodeListRowPreview() {
  PreviewWrapper { EpisodeListRow(episode = Defaults.EDIT_ITEM_PODCAST_UI_STATE.episodes.first()) }
}

@ShelfDroidPreview
@Composable
private fun EpisodesTabPreview() {
  PreviewWrapper { EpisodesTab(uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE) }
}

@ShelfDroidPreview
@Composable
private fun EpisodesTabEmptyPreview() {
  PreviewWrapper {
    EpisodesTab(uiState = Defaults.EDIT_ITEM_PODCAST_UI_STATE.copy(episodes = emptyList()))
  }
}
