@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.prefs.BookSort
import dev.halim.shelfdroid.core.prefs.DisplayPrefs
import dev.halim.shelfdroid.core.prefs.Filter
import dev.halim.shelfdroid.core.prefs.PodcastSort
import dev.halim.shelfdroid.core.prefs.SortOrder
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import kotlinx.coroutines.launch

@Composable
fun DisplayPrefsSheet(
  sheetState: SheetState,
  displayPrefs: DisplayPrefs,
  isBookLibrary: Boolean,
  onFilterChange: (String) -> Unit,
  onBookSortChange: (String) -> Unit,
  onPodcastSortChange: (String) -> Unit,
  onSortOrderChange: (String) -> Unit,
  onPodcastSortOrderChange: (String) -> Unit,
) {
  val scope = rememberCoroutineScope()

  if (sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 64.dp)) {
        MySegmentedButton(
          modifier = Modifier.fillMaxWidth(),
          Filter.entries.map { it.name },
          stringResource(R.string.filter),
          displayPrefs.filter.name,
          onFilterChange,
        )

        val onSortOrderChange = if (isBookLibrary) onSortOrderChange else onPodcastSortOrderChange
        val sortOrderInitialValue =
          if (isBookLibrary) displayPrefs.sortOrder.name else displayPrefs.podcastSortOrder.name
        Spacer(Modifier.width(8.dp))
        MySegmentedButton(
          modifier = Modifier.fillMaxWidth(),
          SortOrder.entries.map { it.name },
          stringResource(R.string.order),
          sortOrderInitialValue,
          onSortOrderChange,
        )

        if (isBookLibrary) {
          SortOptions(
            options = BookSort.entries.map { it.label },
            selected = displayPrefs.bookSort.label,
            onSelected = onBookSortChange,
          )
        } else {
          SortOptions(
            options = PodcastSort.entries.map { it.label },
            selected = displayPrefs.podcastSort.label,
            onSelected = onPodcastSortChange,
          )
        }
      }
    }
  }
}

@Composable
private fun SortOptions(
  options: List<String>,
  selected: String,
  onSelected: (String) -> Unit,
) {
  Text(text = stringResource(R.string.sort), style = MaterialTheme.typography.bodyMedium)
  Column(Modifier.selectableGroup()) {
    options.forEach { option ->
      Row(
        modifier =
          Modifier.fillMaxWidth()
            .selectable(
              selected = option == selected,
              onClick = { onSelected(option) },
              role = Role.RadioButton,
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        RadioButton(selected = option == selected, onClick = null)
        Text(text = option, modifier = Modifier.padding(start = 8.dp))
      }
    }
  }
  Spacer(Modifier.height(16.dp))
}

@ShelfDroidPreview
@Composable
private fun DisplayPrefsSheetPreview() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val displayPrefsSheetState = sheetState(density)

    DisplayPrefsSheet(displayPrefsSheetState, DisplayPrefs(), true, {}, {}, {}, {}, {})
    LaunchedEffect(Unit) { displayPrefsSheetState.show() }
  }
}
