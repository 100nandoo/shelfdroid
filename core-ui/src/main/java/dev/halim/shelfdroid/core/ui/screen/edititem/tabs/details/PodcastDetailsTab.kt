package dev.halim.shelfdroid.core.ui.screen.edititem.tabs.details

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
internal fun PodcastDetailsTab(details: DetailsForm, onEvent: (EditItemEvent) -> Unit) {
  val focusManager = LocalFocusManager.current
  val titleRef = remember { FocusRequester() }
  val authorRef = remember { FocusRequester() }
  val feedRef = remember { FocusRequester() }
  val releaseDateRef = remember { FocusRequester() }
  val itunesIdRef = remember { FocusRequester() }
  val languageRef = remember { FocusRequester() }

  DetailsLayout {
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(titleRef),
      value = details.title,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(title = v) }) },
      label = stringResource(R.string.title),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { authorRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(authorRef),
      value = details.podcastAuthor,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(podcastAuthor = v) }) },
      label = stringResource(R.string.author),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { feedRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(feedRef),
      value = details.rssFeedUrl,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(rssFeedUrl = v) }) },
      label = stringResource(R.string.edit_item_rss_feed_url),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
      onNext = { releaseDateRef.requestFocus() },
    )

    DescriptionField(
      value = details.description,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(description = v) }) },
    )

    GenresAndTags(details = details, onEvent = onEvent)

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(releaseDateRef),
      value = details.releaseDate,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(releaseDate = v) }) },
      label = stringResource(R.string.edit_item_release_date),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { itunesIdRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(itunesIdRef),
      value = details.itunesId,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(itunesId = v) }) },
      label = stringResource(R.string.edit_item_itunes_id),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
      onNext = { languageRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(languageRef),
      value = details.language,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(language = v) }) },
      label = stringResource(R.string.language),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { focusManager.clearFocus() },
    )

    PodcastTypeField(
      value = details.podcastType,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(podcastType = v) }) },
    )

    ExplicitRow(
      checked = details.explicit,
      onCheckedChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(explicit = v) }) },
    )

    SaveRow(onEvent = onEvent)
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PodcastTypeField(value: String, onValueChange: (String) -> Unit) {
  var expanded by remember { mutableStateOf(false) }
  val normalizedValue = value.ifBlank { PODCAST_TYPE_EPISODIC }

  ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
    OutlinedTextField(
      modifier =
        Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      value = podcastTypeLabel(normalizedValue),
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.edit_item_podcast_type)) },
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
    )
    this.ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      PODCAST_TYPES.forEach { type ->
        DropdownMenuItem(
          text = { Text(podcastTypeLabel(type)) },
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
private fun podcastTypeLabel(type: String): String =
  when (type) {
    PODCAST_TYPE_SERIAL -> stringResource(R.string.edit_item_podcast_type_serial)
    else -> stringResource(R.string.edit_item_podcast_type_episodic)
  }

private const val PODCAST_TYPE_EPISODIC = "episodic"
private const val PODCAST_TYPE_SERIAL = "serial"
private val PODCAST_TYPES = listOf(PODCAST_TYPE_EPISODIC, PODCAST_TYPE_SERIAL)

@ShelfDroidPreview
@Composable
private fun PodcastDetailsTabPreview() {
  PreviewWrapper {
    PodcastDetailsTab(details = Defaults.EDIT_ITEM_PODCAST_DETAILS_FORM, onEvent = {})
  }
}
