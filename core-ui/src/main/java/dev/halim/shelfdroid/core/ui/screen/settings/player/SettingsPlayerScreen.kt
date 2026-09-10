package dev.halim.shelfdroid.core.ui.screen.settings.player

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
import dev.halim.shelfdroid.core.data.screen.settings.player.SettingsPlayerUiState
import dev.halim.shelfdroid.core.prefs.CHAPTER_TITLE_PRESET_LINE
import dev.halim.shelfdroid.core.prefs.DEFAULT_SEEK_INTERVAL_SECONDS
import dev.halim.shelfdroid.core.prefs.SEEK_INTERVAL_OPTIONS
import dev.halim.shelfdroid.core.prefs.ChapterTimeDisplay
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsPlayerScreen(viewModel: SettingsPlayerViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsPlayerContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
private fun SettingsPlayerContent(
  uiState: SettingsPlayerUiState = SettingsPlayerUiState(),
  onEvent: (SettingsPlayerEvent) -> Unit = {},
) {
  Column(
    modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.Bottom,
  ) {
    SeekSection(uiState, onEvent)
    ChapterSection(uiState, onEvent)
  }
}

@Composable
private fun SeekSection(uiState: SettingsPlayerUiState, onEvent: (SettingsPlayerEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.seek))
  SeekIntervalDropdown(
    label = stringResource(R.string.seek_back_interval),
    selectedSeconds = uiState.seekBackSeconds,
    onSelected = { onEvent(SettingsPlayerEvent.ChangeSeekBackSeconds(it)) },
  )
  SeekIntervalDropdown(
    label = stringResource(R.string.seek_forward_interval),
    selectedSeconds = uiState.seekForwardSeconds,
    onSelected = { onEvent(SettingsPlayerEvent.ChangeSeekForwardSeconds(it)) },
  )
}

@Composable
private fun SeekIntervalDropdown(
  label: String,
  selectedSeconds: Int,
  onSelected: (Int) -> Unit,
) {
  val seekIntervalOptions = SEEK_INTERVAL_OPTIONS.associateWith { stringResource(R.string.seconds, it) }
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
    label = label,
    labelPosition = LabelPosition.Expand,
    options = seekIntervalOptions.values.toList(),
    initialValue =
      seekIntervalOptions[selectedSeconds]
        ?: seekIntervalOptions.getValue(DEFAULT_SEEK_INTERVAL_SECONDS),
    onClick = { selected ->
      seekIntervalOptions.entries
        .firstOrNull { it.value == selected }
        ?.key
        ?.let(onSelected)
    },
  )
}

@Composable
private fun ChapterSection(uiState: SettingsPlayerUiState, onEvent: (SettingsPlayerEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.chapter))
  val timeRangeLabel = stringResource(R.string.time_range)
  val durationLabel = stringResource(R.string.duration)
  val durationShortLabel = stringResource(R.string.duration_short)
  val chapterTimeDisplayOptions =
    mapOf(
      ChapterTimeDisplay.TimeRange to timeRangeLabel,
      ChapterTimeDisplay.Duration to durationLabel,
      ChapterTimeDisplay.DurationShort to durationShortLabel,
    )
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
    label = stringResource(R.string.chapter_time_display),
    labelPosition = LabelPosition.Expand,
    options = chapterTimeDisplayOptions.values.toList(),
    initialValue = chapterTimeDisplayOptions.getValue(uiState.chapterTimeDisplay),
    onClick = { selected ->
      chapterTimeDisplayOptions.entries
        .firstOrNull { it.value == selected }
        ?.key
        ?.let { onEvent(SettingsPlayerEvent.ChangeChapterTimeDisplay(it)) }
    },
  )
  ChipDropdownMenu(
    modifier = Modifier.fillMaxWidth(),
    label = stringResource(R.string.chapter_title_max_lines),
    labelPosition = LabelPosition.Expand,
    options = CHAPTER_TITLE_PRESET_LINE.map { it.toString() },
    initialValue = uiState.chapterTitleLine.toString(),
    onClick = { selected ->
      selected.toIntOrNull()?.let { onEvent(SettingsPlayerEvent.ChangeChapterTitleLine(it)) }
    },
  )
}

@ShelfDroidPreview
@Composable
fun SettingsPlayerContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsPlayerContent() }
}
