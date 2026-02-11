@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
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

        val options =
          if (isBookLibrary) BookSort.entries.map { it.label }
          else PodcastSort.entries.map { it.label }
        val sortInitialValue =
          if (isBookLibrary) displayPrefs.bookSort.label else displayPrefs.podcastSort.label
        val onSortChange = if (isBookLibrary) onBookSortChange else onPodcastSortChange
        MySegmentedButton(
          modifier = Modifier.fillMaxWidth(),
          options,
          stringResource(R.string.sort),
          sortInitialValue,
          onSortChange,
        )
      }
    }
  }
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
