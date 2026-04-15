package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun DetailsTab(
  details: DetailsForm,
  onEvent: (EditItemEvent) -> Unit,
  seriesSuggestions: List<String> = emptyList(),
) {
  val focusManager = LocalFocusManager.current
  val titleRef = remember { FocusRequester() }
  val subtitleRef = remember { FocusRequester() }
  val publishYearRef = remember { FocusRequester() }
  val isbnRef = remember { FocusRequester() }
  val asinRef = remember { FocusRequester() }
  val publisherRef = remember { FocusRequester() }
  val languageRef = remember { FocusRequester() }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
      onRemove = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(authors = it.authors - v) }) },
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

    OutlinedTextField(
      value = details.description,
      onValueChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(description = v) }) },
      label = { Text(stringResource(R.string.description)) },
      modifier = Modifier.fillMaxWidth(),
      minLines = 4,
      maxLines = 8,
    )

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

    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
        checked = details.explicit,
        onCheckedChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(explicit = v) }) },
      )
      Text(stringResource(R.string.edit_item_explicit))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
      Checkbox(
        checked = details.abridged,
        onCheckedChange = { v -> onEvent(EditItemEvent.UpdateDetails { it.copy(abridged = v) }) },
      )
      Text(stringResource(R.string.edit_item_abridged))
    }

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
}

@ShelfDroidPreview
@Composable
private fun DetailsTabPreview() {
  PreviewWrapper { DetailsTab(details = Defaults.EDIT_ITEM_DETAILS_FORM, onEvent = {}) }
}
