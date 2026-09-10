package dev.halim.shelfdroid.core.ui.screen.settings.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.settings.home.SettingsHomeUiState
import dev.halim.shelfdroid.core.prefs.BookSort
import dev.halim.shelfdroid.core.prefs.Filter
import dev.halim.shelfdroid.core.prefs.PodcastSort
import dev.halim.shelfdroid.core.prefs.SortOrder
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.event.DisplayPrefsEvent
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.settings.SettingsSublabel

@Composable
fun SettingsHomeScreen(viewModel: SettingsHomeViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  SettingsHomeScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun SettingsHomeScreenContent(
  uiState: SettingsHomeUiState = SettingsHomeUiState(),
  onEvent: (SettingsHomeEvent) -> Unit = {},
) {
  Column(
    modifier =
      Modifier.fillMaxSize().padding(vertical = 16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    TextTitleMedium(
      Modifier.padding(top = 16.dp, start = 16.dp, end = 16.dp),
      text = stringResource(R.string.home_screen),
    )
    val paddingStart = Modifier.padding(start = 24.dp, end = 16.dp)
    MySwitch(
      modifier = paddingStart,
      title = stringResource(R.string.list_view),
      checked = uiState.displayPrefs.listView,
      contentDescription = stringResource(R.string.list_view),
      onCheckedChange = { onEvent(SettingsHomeEvent.SwitchListView(it)) },
    )
    MySwitch(
      modifier = paddingStart,
      title = stringResource(R.string.show_only_downloaded),
      checked = uiState.displayPrefs.filter.isDownloaded(),
      contentDescription = stringResource(R.string.show_only_downloaded),
      onCheckedChange = {
        val filter = if (it) Filter.Downloaded else Filter.All
        onEvent(SettingsHomeEvent.DisplayPrefs(DisplayPrefsEvent.Filter(filter.name)))
      },
    )
    if (uiState.canDelete) {
      MySwitch(
        modifier = paddingStart,
        title = stringResource(R.string.user_permanent_delete),
        checked = uiState.crudPrefs.hardDelete,
        contentDescription = stringResource(R.string.user_permanent_delete),
        onCheckedChange = { onEvent(SettingsHomeEvent.SwitchHardDelete(it)) },
      )
    }
    val paddingStartTwo = Modifier.padding(start = 24.dp, top = 4.dp, end = 16.dp)
    SettingsSublabel(
      Modifier.padding(start = 24.dp, top = 4.dp),
      text = stringResource(R.string.book_library),
    )
    ChipDropdownMenu(
      modifier = paddingStartTwo.fillMaxWidth(),
      label = stringResource(R.string.sort),
      labelPosition = LabelPosition.Expand,
      options = BookSort.entries.map { it.label },
      initialValue = uiState.displayPrefs.bookSort.label,
      onClick = {
        onEvent(SettingsHomeEvent.DisplayPrefs(DisplayPrefsEvent.BookSort(it)))
      },
    )
    ChipDropdownMenu(
      modifier = paddingStartTwo.fillMaxWidth(),
      label = stringResource(R.string.order),
      labelPosition = LabelPosition.Expand,
      options = SortOrder.entries.map { it.name },
      initialValue = uiState.displayPrefs.sortOrder.name,
      onClick = { onEvent(SettingsHomeEvent.DisplayPrefs(DisplayPrefsEvent.SortOrder(it))) },
    )

    SettingsSublabel(
      Modifier.padding(start = 24.dp, top = 4.dp),
      text = stringResource(R.string.podcast_library),
    )
    ChipDropdownMenu(
      modifier = paddingStartTwo.fillMaxWidth(),
      label = stringResource(R.string.sort),
      labelPosition = LabelPosition.Expand,
      options = PodcastSort.entries.map { it.label },
      initialValue = uiState.displayPrefs.podcastSort.label,
      onClick = {
        onEvent(SettingsHomeEvent.DisplayPrefs(DisplayPrefsEvent.PodcastSort(it)))
      },
    )
    ChipDropdownMenu(
      modifier = paddingStartTwo.fillMaxWidth(),
      label = stringResource(R.string.order),
      labelPosition = LabelPosition.Expand,
      options = SortOrder.entries.map { it.name },
      initialValue = uiState.displayPrefs.podcastSortOrder.name,
      onClick = {
        onEvent(SettingsHomeEvent.DisplayPrefs(DisplayPrefsEvent.PodcastSortOrder(it)))
      },
    )
  }
}

@ShelfDroidPreview
@Composable
private fun SettingsHomeScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SettingsHomeScreenContent(uiState = SettingsHomeUiState(canDelete = true))
  }
}
