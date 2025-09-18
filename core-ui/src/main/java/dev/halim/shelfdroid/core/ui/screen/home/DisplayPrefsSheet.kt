@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.BookSort
import dev.halim.shelfdroid.core.DisplayPrefs
import dev.halim.shelfdroid.core.Filter
import dev.halim.shelfdroid.core.PodcastSort
import dev.halim.shelfdroid.core.SortOrder
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExposedDropdownMenu
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
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
) {
  val scope = rememberCoroutineScope()

  if (sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      Column(Modifier.padding(start = 64.dp, end = 64.dp, bottom = 64.dp)) {
        ExposedDropdownMenu(
          modifier = Modifier.fillMaxWidth(),
          Filter.entries.map { it.name },
          stringResource(R.string.filter),
          displayPrefs.filter.name,
          onFilterChange,
        )
        Row {
          val options =
            if (isBookLibrary) BookSort.entries.map { it.label }
            else PodcastSort.entries.map { it.label }
          val initialValue =
            if (isBookLibrary) displayPrefs.bookSort.label else displayPrefs.podcastSort.label
          val onClick = if (isBookLibrary) onBookSortChange else onPodcastSortChange
          ExposedDropdownMenu(
            modifier = Modifier.weight(1f),
            options,
            stringResource(R.string.sort),
            initialValue,
            onClick,
          )
          Spacer(Modifier.width(8.dp))
          ExposedDropdownMenu(
            modifier = Modifier.weight(1f),
            SortOrder.entries.map { it.name },
            stringResource(R.string.order),
            displayPrefs.sortOrder.name,
            onSortOrderChange,
          )
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun DisplayPrefsSheetPreview() {
  PreviewWrapper(false) {
    val density = LocalDensity.current
    val displayPrefsSheetState =
      SheetState(
        skipPartiallyExpanded = true,
        initialValue = SheetValue.Expanded,
        density = density,
      )

    DisplayPrefsSheet(displayPrefsSheetState, DisplayPrefs(), true, {}, {}, {}, {})
    LaunchedEffect(Unit) { displayPrefsSheetState.show() }
  }
}
