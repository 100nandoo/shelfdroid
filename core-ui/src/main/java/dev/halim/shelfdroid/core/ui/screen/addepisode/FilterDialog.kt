package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import dev.halim.shelfdroid.core.data.screen.addepisode.TextFilter
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun FilterDialog(
  showDialog: Boolean,
  filterState: AddEpisodeFilterState = AddEpisodeFilterState(),
  onConfirm: () -> Unit,
  onDismiss: () -> Unit = {},
  onEvent: (AddEpisodeEvent.FilterEvent) -> Unit = {},
) {
  if (showDialog) {
    AlertDialog(
      onDismissRequest = { onDismiss() },
      title = {
        Text(
          text = stringResource(R.string.filter),
          textAlign = TextAlign.Center,
          modifier = Modifier.fillMaxWidth(),
        )
      },
      text = {
        Column {
          MyOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = filterState.text,
            onValueChange = { onEvent(AddEpisodeEvent.FilterEvent.TextChanged(it)) },
            label = stringResource(R.string.search),
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
          )
          Spacer(modifier = Modifier.height(16.dp))

          MultiChoiceSegmentedButton(filterState.textFilter, onEvent = onEvent)

          Spacer(modifier = Modifier.height(16.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
          ) {
            Checkbox(
              checked = filterState.hideDownloaded,
              onCheckedChange = { onEvent(AddEpisodeEvent.FilterEvent.HideDownloadedChanged(it)) },
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.hide_downloaded_episode))
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { onConfirm() }) { Text(stringResource(R.string.ok)) }
      },
    )
  }
}

@Composable
fun MultiChoiceSegmentedButton(
  textFilter: TextFilter,
  onEvent: (AddEpisodeEvent.FilterEvent) -> Unit,
) {
  val selectedOptions = textFilter.toSelectionList()
  val options = listOf(stringResource(R.string.title), stringResource(R.string.description))
  Text(
    stringResource(R.string.search_in),
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier.padding(bottom = 4.dp),
  )
  MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    options.forEachIndexed { index, label ->
      SegmentedButton(
        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
        checked = selectedOptions[index],
        onCheckedChange = {
          val newSelection =
            selectedOptions.toMutableList().apply {
              this[index] = !this[index]
              if (!contains(true)) this[0] = true
            }
          onEvent(AddEpisodeEvent.FilterEvent.TextFilterChanged(newSelection.toTextFilter()))
        },
        icon = { SegmentedButtonDefaults.Icon(selectedOptions[index]) },
        label = { Text(label) },
      )
    }
  }
}

private fun List<Boolean>.toTextFilter(): TextFilter {
  val (titleSelected, descriptionSelected) = this
  return when {
    titleSelected && descriptionSelected -> TextFilter.BOTH
    titleSelected -> TextFilter.TITLE
    descriptionSelected -> TextFilter.DESCRIPTION
    else -> TextFilter.TITLE
  }
}

private fun TextFilter.toSelectionList(): SnapshotStateList<Boolean> =
  when (this) {
    TextFilter.TITLE -> mutableStateListOf(true, false)
    TextFilter.DESCRIPTION -> mutableStateListOf(false, true)
    TextFilter.BOTH -> mutableStateListOf(true, true)
  }

@ShelfDroidPreview
@Composable
fun FilterDialogPreview() {
  AnimatedPreviewWrapper { FilterDialog(showDialog = true, onConfirm = {}, onDismiss = {}) }
}
