@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.finishThreshold
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateEvent
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminFinishThresholdMode
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.createErrorText

@Composable
internal fun LibraryAdminSettingsTab(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  val isBook = uiState.draft.mediaType == MediaType.BOOK
  val bookSettings = uiState.draft.bookSettings
  val podcastSettings = uiState.draft.podcastSettings
  val coverAspectRatio =
    if (isBook) bookSettings.coverAspectRatio else podcastSettings.coverAspectRatio
  val disableWatcher = if (isBook) bookSettings.disableWatcher else podcastSettings.disableWatcher
  val finishThreshold =
    if (isBook) bookSettings.finishThreshold else podcastSettings.finishThreshold
  val finishPercent = finishThreshold.percentComplete
  val finishMode =
    if (finishPercent != null) {
      LibraryAdminFinishThresholdMode.PERCENT_COMPLETE
    } else {
      LibraryAdminFinishThresholdMode.TIME_REMAINING
    }
  val finishValue = finishThreshold.value
  val finishError = uiState.validation.errors[LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD]

  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .focusRequester(focusRequester)
        .focusable(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_settings_covers_heading))
    MySwitch(
      title = stringResource(R.string.library_settings_square_covers),
      checked = coverAspectRatio == 1,
      contentDescription = stringResource(R.string.library_settings_square_covers),
      onCheckedChange = { enabled ->
        onEvent(LibraryAdminCreateEvent.UpdateCoverAspectRatio(if (enabled) 1 else 0))
      },
    )
    MySwitch(
      title = stringResource(R.string.library_settings_watcher),
      checked = !disableWatcher,
      contentDescription = stringResource(R.string.library_settings_watcher),
      onCheckedChange = { enabled ->
        onEvent(LibraryAdminCreateEvent.UpdateWatcher(enabled))
      },
    )

    if (isBook) {
      Text(stringResource(R.string.library_settings_book_heading))
      MySwitch(
        title = stringResource(R.string.library_settings_audiobooks_only),
        checked = bookSettings.audiobooksOnly,
        contentDescription = stringResource(R.string.library_settings_audiobooks_only),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateAudiobooksOnly(it)) },
      )
      MySwitch(
        title = stringResource(R.string.library_settings_skip_asin),
        checked = bookSettings.skipMatchingMediaWithAsin,
        contentDescription = stringResource(R.string.library_settings_skip_asin),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateSkipMatchingAsin(it)) },
      )
      MySwitch(
        title = stringResource(R.string.library_settings_skip_isbn),
        checked = bookSettings.skipMatchingMediaWithIsbn,
        contentDescription = stringResource(R.string.library_settings_skip_isbn),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateSkipMatchingIsbn(it)) },
      )
      MySwitch(
        title = stringResource(R.string.library_settings_hide_single_series),
        checked = bookSettings.hideSingleBookSeries,
        contentDescription = stringResource(R.string.library_settings_hide_single_series),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateHideSingleBookSeries(it)) },
      )
      MySwitch(
        title = stringResource(R.string.library_settings_only_later_books),
        checked = bookSettings.onlyShowLaterBooksInContinueSeries,
        contentDescription = stringResource(R.string.library_settings_only_later_books),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateOnlyShowLaterBooks(it)) },
      )
      MySwitch(
        title = stringResource(R.string.library_settings_scripted_epub),
        checked = bookSettings.epubsAllowScriptedContent,
        contentDescription = stringResource(R.string.library_settings_scripted_epub),
        onCheckedChange = { onEvent(LibraryAdminCreateEvent.UpdateScriptedEpubs(it)) },
      )
      if (bookSettings.epubsAllowScriptedContent) {
        val warningDescription = stringResource(R.string.library_settings_scripted_epub_warning)
        Text(
          text = warningDescription,
          modifier = Modifier.semantics { contentDescription = warningDescription },
        )
      }
    } else {
      LibraryAdminPodcastRegionPicker(
        region = podcastSettings.podcastSearchRegion,
        onRegionSelected = {
          onEvent(LibraryAdminCreateEvent.UpdatePodcastSearchRegion(it))
        },
      )
    }

    Text(stringResource(R.string.library_settings_finish_heading))
    val timeRemainingLabel = stringResource(R.string.library_settings_finish_time)
    val percentCompleteLabel = stringResource(R.string.library_settings_finish_percent)
    MySegmentedButton(
      options =
        listOf(
          LibraryAdminFinishThresholdMode.TIME_REMAINING,
          LibraryAdminFinishThresholdMode.PERCENT_COMPLETE,
        ),
      selectedValue = finishMode,
      optionLabel = { mode ->
        if (mode == LibraryAdminFinishThresholdMode.TIME_REMAINING) timeRemainingLabel
        else percentCompleteLabel
      },
      onClick = { onEvent(LibraryAdminCreateEvent.SelectFinishThresholdMode(it)) },
    )
    OutlinedTextField(
      value = finishValue.toString(),
      onValueChange = { value ->
        value.toIntOrNull()?.let {
          onEvent(LibraryAdminCreateEvent.UpdateFinishThresholdValue(it))
        }
      },
      label = {
        Text(
          if (finishMode == LibraryAdminFinishThresholdMode.TIME_REMAINING) {
            stringResource(R.string.library_settings_finish_seconds)
          } else {
            stringResource(R.string.library_settings_finish_percent)
          }
        )
      },
      modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
      isError = finishError != null,
      supportingText =
        finishError?.let { errors ->
          { Text(createErrorText(errors)) }
        },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
  }
}

@Composable
private fun LibraryAdminPodcastRegionPicker(
  region: String,
  onRegionSelected: (String) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  val regions =
    listOf(
      "au" to "Australia",
      "br" to "Brasil",
      "be" to "België / Belgique / Belgien",
      "by" to "Беларусь",
      "cz" to "Česko",
      "dk" to "Danmark",
      "de" to "Deutschland",
      "ee" to "Eesti",
      "es" to "España / Espanya / Espainia",
      "fr" to "France",
      "hr" to "Hrvatska",
      "il" to "ישראל / إسرائيل",
      "it" to "Italia",
      "jp" to "日本",
      "lu" to "Luxembourg / Luxemburg / Lëtezebuerg",
      "hu" to "Magyarország",
      "nl" to "Nederland",
      "no" to "Norge",
      "nz" to "New Zealand",
      "at" to "Österreich",
      "pl" to "Polska",
      "pt" to "Portugal",
      "ru" to "Россия",
      "ch" to "Schweiz / Suisse / Svizzera",
      "sk" to "Slovensko",
      "se" to "Sverige",
      "vn" to "Việt Nam",
      "ua" to "Україна",
      "gb" to "United Kingdom",
      "us" to "United States",
      "cn" to "中国",
    )
  val selectedRegion = regions.firstOrNull { it.first == region } ?: (region to region)
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
  ) {
    OutlinedTextField(
      value = selectedRegion.second,
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.library_settings_podcast_region)) },
      modifier =
        Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      regions.forEach { (code, name) ->
        DropdownMenuItem(
          text = { Text(name) },
          onClick = {
            onRegionSelected(code)
            expanded = false
          },
        )
      }
    }
  }
}
