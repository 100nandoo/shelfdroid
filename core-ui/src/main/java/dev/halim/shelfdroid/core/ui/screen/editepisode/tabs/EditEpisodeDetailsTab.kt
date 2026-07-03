package dev.halim.shelfdroid.core.ui.screen.editepisode.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.editepisode.EpisodeDetailsForm
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.DateTimePickerTextField
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.details.DescriptionField
import dev.halim.shelfdroid.core.ui.screen.edititem.tabs.details.DetailsLayout
import dev.halim.shelfdroid.core.ui.screen.editepisode.EditEpisodeEvent

@Composable
internal fun EditEpisodeDetailsTab(
  podcastTitle: String,
  details: EpisodeDetailsForm,
  canSave: Boolean,
  onEvent: (EditEpisodeEvent) -> Unit,
  onSave: () -> Unit,
) {
  val focusManager = LocalFocusManager.current
  val seasonRef = remember { FocusRequester() }
  val episodeRef = remember { FocusRequester() }
  val titleRef = remember { FocusRequester() }

  DetailsLayout {
    if (podcastTitle.isNotBlank()) {
      Text(
        text = podcastTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = if (podcastTitle.isBlank()) 16.dp else 0.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      MyOutlinedTextField(
        modifier = Modifier.weight(1f).focusRequester(seasonRef),
        value = details.season,
        onValueChange = { value ->
          onEvent(EditEpisodeEvent.UpdateDetails { it.copy(season = value) })
        },
        label = stringResource(R.string.season),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        onNext = { episodeRef.requestFocus() },
      )
      MyOutlinedTextField(
        modifier = Modifier.weight(1f).focusRequester(episodeRef),
        value = details.episode,
        onValueChange = { value ->
          onEvent(EditEpisodeEvent.UpdateDetails { it.copy(episode = value) })
        },
        label = stringResource(R.string.edit_episode_number),
        keyboardOptions =
          KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        onNext = { titleRef.requestFocus() },
      )
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Top,
    ) {
      EpisodeTypeField(
        modifier = Modifier.weight(1f),
        value = details.episodeType,
        onValueChange = { value ->
          onEvent(EditEpisodeEvent.UpdateDetails { it.copy(episodeType = value) })
        },
      )
      DateTimePickerTextField(
        modifier = Modifier.weight(2f),
        label = stringResource(R.string.edit_episode_published_date),
        selectedDateTimeMillis = details.publishedAtMillis,
        onDateTimeSelected = { value ->
          onEvent(EditEpisodeEvent.UpdateDetails { it.copy(publishedAtMillis = value) })
        },
      )
    }

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(titleRef),
      value = details.title,
      onValueChange = { value ->
        onEvent(EditEpisodeEvent.UpdateDetails { it.copy(title = value) })
      },
      label = stringResource(R.string.title),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { focusManager.clearFocus() },
    )

    OutlinedTextField(
      value = details.subtitle,
      onValueChange = { value ->
        onEvent(EditEpisodeEvent.UpdateDetails { it.copy(subtitle = value) })
      },
      label = { Text(stringResource(R.string.edit_item_subtitle)) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 3,
      maxLines = 6,
    )

    DescriptionField(
      value = details.description,
      onValueChange = { value ->
        onEvent(EditEpisodeEvent.UpdateDetails { it.copy(description = value) })
      },
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save)) }
    }

    if (details.enclosureUrl.isNotBlank()) {
      OutlinedTextField(
        value = details.enclosureUrl,
        onValueChange = {},
        readOnly = true,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        label = { Text(stringResource(R.string.edit_episode_rss_enclosure_url)) },
      )
    } else {
      Text(
        text = stringResource(R.string.edit_episode_not_linked_to_rss_feed),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 16.dp),
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeTypeField(
  modifier: Modifier = Modifier,
  value: String,
  onValueChange: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
      modifier =
        modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      value = episodeTypeLabel(value),
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.edit_episode_type)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      EPISODE_TYPES.forEach { type ->
        DropdownMenuItem(
          text = { Text(episodeTypeLabel(type)) },
          onClick = {
            onValueChange(type)
            expanded = false
          },
        )
      }
    }
  }
}

@Composable
private fun episodeTypeLabel(type: String): String =
  when (type) {
    EPISODE_TYPE_FULL -> stringResource(R.string.edit_episode_type_full)
    EPISODE_TYPE_TRAILER -> stringResource(R.string.edit_episode_type_trailer)
    EPISODE_TYPE_BONUS -> stringResource(R.string.edit_episode_type_bonus)
    else -> ""
  }

private const val EPISODE_TYPE_FULL = "full"
private const val EPISODE_TYPE_TRAILER = "trailer"
private const val EPISODE_TYPE_BONUS = "bonus"
private val EPISODE_TYPES = listOf(EPISODE_TYPE_FULL, EPISODE_TYPE_TRAILER, EPISODE_TYPE_BONUS)

@ShelfDroidPreview
@Composable
private fun EditEpisodeDetailsTabPreview() {
  PreviewWrapper {
    EditEpisodeDetailsTab(
      podcastTitle = "Daily Pod",
      details =
        EpisodeDetailsForm(
          season = "2",
          episode = "12",
          episodeType = EPISODE_TYPE_BONUS,
          publishedAtMillis = 1781818200000L,
          title = "Episode title",
          subtitle = "Subtitle",
          description = "<p>Raw HTML description</p>",
          enclosureUrl = "https://example.com/episode.mp3",
        ),
      canSave = true,
      onEvent = {},
      onSave = {},
    )
  }
}
