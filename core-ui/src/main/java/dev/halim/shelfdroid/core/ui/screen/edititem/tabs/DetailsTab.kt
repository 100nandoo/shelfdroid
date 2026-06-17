package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedButton
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
import dev.halim.shelfdroid.core.data.screen.edititem.DetailsForm
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemMediaKind
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun DetailsTab(
  mediaKind: EditItemMediaKind,
  details: DetailsForm,
  onEvent: (EditItemEvent) -> Unit,
  seriesSuggestions: List<String> = emptyList(),
) {
  when (mediaKind) {
    EditItemMediaKind.Book ->
      BookDetailsTab(details = details, onEvent = onEvent, seriesSuggestions = seriesSuggestions)
    EditItemMediaKind.Podcast -> PodcastDetailsTab(details = details, onEvent = onEvent)
  }
}

@Composable
private fun BookDetailsTab(
  details: DetailsForm,
  onEvent: (EditItemEvent) -> Unit,
  seriesSuggestions: List<String>,
) {
  val focusManager = LocalFocusManager.current
  val titleRef = remember { FocusRequester() }
  val subtitleRef = remember { FocusRequester() }
  val publishYearRef = remember { FocusRequester() }
  val isbnRef = remember { FocusRequester() }
  val asinRef = remember { FocusRequester() }
  val publisherRef = remember { FocusRequester() }
  val languageRef = remember { FocusRequester() }

  DetailsLayout {
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(titleRef),
      value = details.title,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(title = v) }) },
      label = stringResource(R.string.title),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { subtitleRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(subtitleRef),
      value = details.subtitle,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(subtitle = v) }) },
      label = stringResource(R.string.edit_item_subtitle),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { publishYearRef.requestFocus() },
    )

    ChipInput(
      label = stringResource(R.string.edit_item_authors),
      values = details.authors,
      onAdd = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(authors = it.authors + v) }) },
      onRemove = { v ->
        onEvent(EditItemEvent.UpdateDetails { it.copy(authors = it.authors - v) })
      },
    )

    ChipInput(
      label = stringResource(R.string.edit_item_narrators),
      values = details.narrators,
      onAdd = { v ->
        onEvent(EditItemEvent.UpdateDetails { it.copy(narrators = it.narrators + v) })
      },
      onRemove = { v ->
        onEvent(EditItemEvent.UpdateDetails { it.copy(narrators = it.narrators - v) })
      },
    )

    SeriesInput(
      label = stringResource(R.string.edit_item_series),
      values = details.series,
      suggestions = seriesSuggestions,
      onAdd = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(series = it.series + v) }) },
      onRemove = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(series = it.series - v) }) },
    )

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(publishYearRef),
      value = details.publishedYear,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(publishedYear = v) }) },
      label = stringResource(R.string.edit_item_publish_year),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
      onNext = { isbnRef.requestFocus() },
    )

    DescriptionField(
      value = details.description,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(description = v) }) },
    )

    GenresAndTags(details = details, onEvent = onEvent)

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(isbnRef),
      value = details.isbn,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(isbn = v) }) },
      label = stringResource(R.string.edit_item_isbn),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { asinRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(asinRef),
      value = details.asin,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(asin = v) }) },
      label = stringResource(R.string.edit_item_asin),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { publisherRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(publisherRef),
      value = details.publisher,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(publisher = v) }) },
      label = stringResource(R.string.publisher),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
      onNext = { languageRef.requestFocus() },
    )
    MyOutlinedTextField(
      modifier = Modifier.focusRequester(languageRef),
      value = details.language,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(language = v) }) },
      label = stringResource(R.string.language),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
      onDone = { focusManager.clearFocus() },
    )

    ExplicitRow(
      checked = details.explicit,
      onCheckedChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(explicit = v) }) },
    )
    CheckboxRow(
      checked = details.abridged,
      text = stringResource(R.string.edit_item_abridged),
      onCheckedChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(abridged = v) }) },
    )

    BookActionRow(onEvent = onEvent)
  }
}

@Composable
private fun PodcastDetailsTab(details: DetailsForm, onEvent: (EditItemEvent) -> Unit) {
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

@Composable
private fun DetailsLayout(content: @Composable ColumnScope.() -> Unit) {
  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    content = content,
  )
}

@Composable
private fun DescriptionField(value: String, onValueChange: (String) -> Unit) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(stringResource(R.string.description)) },
    modifier = Modifier.fillMaxWidth(),
    minLines = 4,
    maxLines = 8,
  )
}

@Composable
private fun GenresAndTags(details: DetailsForm, onEvent: (EditItemEvent) -> Unit) {
  ChipInput(
    label = stringResource(R.string.genres),
    values = details.genres,
    onAdd = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(genres = it.genres + v) }) },
    onRemove = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(genres = it.genres - v) }) },
  )

  ChipInput(
    label = stringResource(R.string.edit_item_tags),
    values = details.tags,
    onAdd = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(tags = it.tags + v) }) },
    onRemove = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(tags = it.tags - v) }) },
  )
}

@Composable
private fun ExplicitRow(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
  CheckboxRow(
    checked = checked,
    text = stringResource(R.string.edit_item_explicit),
    onCheckedChange = onCheckedChange,
  )
}

@Composable
private fun CheckboxRow(checked: Boolean, text: String, onCheckedChange: (Boolean) -> Unit) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    Text(text)
  }
}

@Composable
private fun BookActionRow(onEvent: (EditItemEvent) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedButton(onClick = { onEvent(EditItemEvent.QuickMatch) }) {
      Text(stringResource(R.string.edit_item_quick_match))
    }
    Button(onClick = { onEvent(EditItemEvent.Save) }) { Text(stringResource(R.string.save)) }
  }
}

@Composable
private fun SaveRow(onEvent: (EditItemEvent) -> Unit) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    horizontalArrangement = Arrangement.End,
  ) {
    Button(onClick = { onEvent(EditItemEvent.Save) }) { Text(stringResource(R.string.save)) }
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
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
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
private fun BookDetailsTabPreview() {
  PreviewWrapper {
    DetailsTab(
      mediaKind = EditItemMediaKind.Book,
      details = Defaults.EDIT_ITEM_DETAILS_FORM,
      onEvent = {},
    )
  }
}

@ShelfDroidPreview
@Composable
private fun PodcastDetailsTabPreview() {
  PreviewWrapper {
    DetailsTab(
      mediaKind = EditItemMediaKind.Podcast,
      details = Defaults.EDIT_ITEM_PODCAST_DETAILS_FORM,
      onEvent = {},
    )
  }
}
