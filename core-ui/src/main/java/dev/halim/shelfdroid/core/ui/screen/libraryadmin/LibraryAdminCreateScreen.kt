@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminDirectory
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.finishThreshold
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminProviderState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleInterval
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleMode
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminScheduleValidationState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.nextLibraryScheduleRun
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdminCreateScreen(
  viewModel: LibraryAdminCreateViewModel = hiltViewModel(),
  onNavigateBack: () -> Unit = {},
  onCreated: (String) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler { viewModel.onEvent(LibraryAdminCreateEvent.Back) }

  LaunchedEffect(uiState.navigation) {
    when (val navigation = uiState.navigation) {
      LibraryAdminCreateNavigation.Back -> {
        viewModel.onEvent(LibraryAdminCreateEvent.ConsumeNavigation)
        onNavigateBack()
      }
      is LibraryAdminCreateNavigation.Created -> {
        viewModel.onEvent(LibraryAdminCreateEvent.ConsumeNavigation)
        onCreated(navigation.library.id)
      }
      null -> Unit
    }
  }

  LibraryAdminCreateContent(uiState, viewModel::onEvent)
}

@Composable
internal fun LibraryAdminCreateContent(
  uiState: LibraryAdminCreateUiState = LibraryAdminCreateUiState(),
  onEvent: (LibraryAdminCreateEvent) -> Unit = {},
) {
  val nameFocusRequester = remember { FocusRequester() }
  val providerFocusRequester = remember { FocusRequester() }
  val folderFocusRequester = remember { FocusRequester() }
  val settingsFocusRequester = remember { FocusRequester() }
  val scannerFocusRequester = remember { FocusRequester() }
  val scheduleFocusRequester = remember { FocusRequester() }

  LaunchedEffect(uiState.focusField) {
    when (uiState.focusField) {
      LibraryAdminCreateField.NAME -> nameFocusRequester.requestFocus()
      LibraryAdminCreateField.PROVIDER -> providerFocusRequester.requestFocus()
      LibraryAdminCreateField.FOLDERS -> folderFocusRequester.requestFocus()
      LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD -> settingsFocusRequester.requestFocus()
      LibraryAdminCreateField.SCANNER_PRECEDENCE -> scannerFocusRequester.requestFocus()
      LibraryAdminCreateField.SCHEDULE -> scheduleFocusRequester.requestFocus()
      else -> Unit
    }
    if (uiState.focusField != null) onEvent(LibraryAdminCreateEvent.ConsumeFocus)
  }

  Column(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())
    ) {
      Text(
        text = stringResource(R.string.create_library),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      )
      when (uiState.selectedTab) {
        LibraryAdminCreateTab.DETAILS ->
          LibraryAdminDetailsContent(
            uiState = uiState,
            onEvent = onEvent,
            nameFocusRequester = nameFocusRequester,
            providerFocusRequester = providerFocusRequester,
            folderFocusRequester = folderFocusRequester,
          )
        LibraryAdminCreateTab.SETTINGS ->
          LibraryAdminSettingsContent(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = settingsFocusRequester,
          )
        LibraryAdminCreateTab.SCANNER ->
          LibraryAdminScannerContent(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = scannerFocusRequester,
          )
        LibraryAdminCreateTab.SCHEDULE ->
          LibraryAdminScheduleContent(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = scheduleFocusRequester,
          )
      }
    }

    val visibleTabs =
      LibraryAdminCreateTab.entries.filter {
        it != LibraryAdminCreateTab.SCANNER ||
          uiState.draft.mediaType != LibraryAdminMediaType.PODCAST
      }
    ScrollableTabRow(
      selectedTabIndex = visibleTabs.indexOf(uiState.selectedTab).coerceAtLeast(0),
      modifier = Modifier.fillMaxWidth(),
    ) {
      visibleTabs.forEach { tab ->
        Tab(
          selected = uiState.selectedTab == tab,
          onClick = { onEvent(LibraryAdminCreateEvent.SelectTab(tab)) },
          text = { Text(createTabLabel(tab)) },
        )
      }
    }

    Button(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      enabled = !uiState.isBusy,
      onClick = { onEvent(LibraryAdminCreateEvent.Submit) },
    ) {
      if (uiState.isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
      }
      Text(stringResource(R.string.create_library))
    }

    when (val submission = uiState.submissionState) {
      is LibraryAdminCreateSubmissionState.ServerFailure ->
        Text(
          text = stringResource(R.string.library_create_server_failed),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
      is LibraryAdminCreateSubmissionState.LocalSyncFailure -> {
        Text(
          text = stringResource(R.string.library_create_local_sync_failed),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextButton(
          onClick = { onEvent(LibraryAdminCreateEvent.RetryLocalSynchronization) },
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(stringResource(R.string.retry))
        }
      }
      else -> Unit
    }
  }

  if (uiState.discardDialog) {
    AlertDialog(
      onDismissRequest = { onEvent(LibraryAdminCreateEvent.CancelDiscard) },
      title = { Text(stringResource(R.string.library_create_discard_title)) },
      text = { Text(stringResource(R.string.library_create_discard_message)) },
      confirmButton = {
        TextButton(onClick = { onEvent(LibraryAdminCreateEvent.ConfirmDiscard) }) {
          Text(stringResource(R.string.library_create_discard))
        }
      },
      dismissButton = {
        TextButton(onClick = { onEvent(LibraryAdminCreateEvent.CancelDiscard) }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  when (val filesystem = uiState.filesystemState) {
    is LibraryAdminFilesystemState.Success ->
      LibraryAdminFilesystemDialog(filesystem, onEvent)
    is LibraryAdminFilesystemState.Failure ->
      AlertDialog(
        onDismissRequest = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) },
        title = { Text(stringResource(R.string.library_filesystem_browser)) },
        text = {
          Text(stringResource(R.string.library_filesystem_load_failed))
        },
        confirmButton = {
          TextButton(
            onClick = {
              onEvent(
                LibraryAdminCreateEvent.OpenFilesystemPath(filesystem.path.orEmpty())
              )
            }
          ) {
            Text(stringResource(R.string.retry))
          }
        },
        dismissButton = {
          TextButton(onClick = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    is LibraryAdminFilesystemState.Loading ->
      AlertDialog(
        onDismissRequest = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) },
        title = { Text(stringResource(R.string.library_filesystem_browser)) },
        text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
        confirmButton = {},
      )
    LibraryAdminFilesystemState.Closed -> Unit
  }
}

@Composable
internal fun LibraryAdminDetailsContent(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  nameFocusRequester: FocusRequester,
  providerFocusRequester: FocusRequester,
  folderFocusRequester: FocusRequester,
) {
  val errors = uiState.validation.errors
  Column(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_create_media_type))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = uiState.draft.mediaType == LibraryAdminMediaType.BOOK,
        onClick = {
          onEvent(
            LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.BOOK)
          )
        },
        label = { Text(stringResource(R.string.book_library)) },
      )
      FilterChip(
        selected = uiState.draft.mediaType == LibraryAdminMediaType.PODCAST,
        onClick = {
          onEvent(
            LibraryAdminCreateEvent.SelectMediaType(LibraryAdminMediaType.PODCAST)
          )
        },
        label = { Text(stringResource(R.string.podcast_library)) },
      )
    }

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(nameFocusRequester),
      value = uiState.draft.name,
      onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateName(it)) },
      label = stringResource(R.string.library_create_name),
      supportingText =
        errors[LibraryAdminCreateField.NAME]?.let { nameErrors ->
          createErrorText(nameErrors)
        },
      isError = errors.containsKey(LibraryAdminCreateField.NAME),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )

    Text(stringResource(R.string.library_create_icon))
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LibraryAdminDraft.ICON_IDS.forEach { icon ->
        FilterChip(
          selected = uiState.draft.icon == icon,
          onClick = { onEvent(LibraryAdminCreateEvent.SelectIcon(icon)) },
          label = { Text(icon) },
          leadingIcon = {
            Icon(
              painter = painterResource(libraryIconResource(icon)),
              contentDescription = icon,
            )
          },
        )
      }
    }

    LibraryAdminProviderPicker(uiState, onEvent, providerFocusRequester)

    Text(stringResource(R.string.library_create_folders))
    uiState.draft.folders.forEach { folder ->
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(folder, modifier = Modifier.weight(1f))
        IconButton(onClick = { onEvent(LibraryAdminCreateEvent.RemoveFolder(folder)) }) {
          Icon(
            painter = painterResource(R.drawable.close),
            contentDescription = stringResource(R.string.library_remove_folder),
          )
        }
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
      OutlinedTextField(
        modifier = Modifier.weight(1f).focusRequester(folderFocusRequester),
        value = uiState.manualFolderDraft,
        onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateManualFolder(it)) },
        label = { Text(stringResource(R.string.library_folder_path)) },
        isError = errors.containsKey(LibraryAdminCreateField.FOLDERS),
        supportingText =
          errors[LibraryAdminCreateField.FOLDERS]?.let { folderErrors ->
            { Text(createErrorText(folderErrors)) }
          },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
      )
      Spacer(Modifier.width(8.dp))
      TextButton(
        onClick = { onEvent(LibraryAdminCreateEvent.AddManualFolder) },
        modifier = Modifier.padding(top = 4.dp),
      ) {
        Text(stringResource(R.string.add))
      }
    }
    TextButton(
      onClick = { onEvent(LibraryAdminCreateEvent.OpenFilesystem) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.library_browse_server_folders))
    }
  }
}

@Composable
internal fun LibraryAdminSettingsContent(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  val isBook = uiState.draft.mediaType == LibraryAdminMediaType.BOOK
  val bookSettings = uiState.draft.bookSettings
  val podcastSettings = uiState.draft.podcastSettings
  val coverAspectRatio = if (isBook) bookSettings.coverAspectRatio else podcastSettings.coverAspectRatio
  val disableWatcher = if (isBook) bookSettings.disableWatcher else podcastSettings.disableWatcher
  val finishThreshold = if (isBook) bookSettings.finishThreshold else podcastSettings.finishThreshold
  val finishPercent = finishThreshold.percentComplete
  val finishMode =
    if (finishPercent != null) {
      LibraryAdminFinishThresholdMode.PERCENT_COMPLETE
    } else {
      LibraryAdminFinishThresholdMode.TIME_REMAINING
    }
  val finishValue = finishThreshold.value
  val finishError =
    uiState.validation.errors[LibraryAdminCreateField.SETTINGS_FINISH_THRESHOLD]

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
        onEvent(
          LibraryAdminCreateEvent.UpdateCoverAspectRatio(if (enabled) 1 else 0)
        )
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
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      FilterChip(
        selected = finishMode == LibraryAdminFinishThresholdMode.TIME_REMAINING,
        onClick = {
          onEvent(
            LibraryAdminCreateEvent.SelectFinishThresholdMode(
              LibraryAdminFinishThresholdMode.TIME_REMAINING
            )
          )
        },
        label = { Text(stringResource(R.string.library_settings_finish_time)) },
      )
      FilterChip(
        selected = finishMode == LibraryAdminFinishThresholdMode.PERCENT_COMPLETE,
        onClick = {
          onEvent(
            LibraryAdminCreateEvent.SelectFinishThresholdMode(
              LibraryAdminFinishThresholdMode.PERCENT_COMPLETE
            )
          )
        },
        label = { Text(stringResource(R.string.library_settings_finish_percent)) },
      )
    }
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
      supportingText = finishError?.let { errors ->
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
      modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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

@Composable
internal fun LibraryAdminScheduleContent(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  val schedule = uiState.draft.schedule
  val scheduleError = uiState.validation.errors[LibraryAdminCreateField.SCHEDULE]
  val errorDescription = scheduleError?.let { createErrorText(it) }
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .focusRequester(focusRequester)
        .focusable(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_schedule_heading))
    MySwitch(
      title = stringResource(R.string.library_schedule_enable),
      checked = schedule.enabled,
      contentDescription = stringResource(R.string.library_schedule_enable),
      onCheckedChange = { onEvent(LibraryAdminCreateEvent.ToggleSchedule(it)) },
    )
    if (!schedule.enabled) {
      val disabledDescription = stringResource(R.string.library_schedule_disabled_note)
      Text(
        text = disabledDescription,
        modifier = Modifier.semantics { contentDescription = disabledDescription },
      )
    } else {
      FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = schedule.mode == LibraryAdminScheduleMode.Simple,
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Simple))
          },
          label = { Text(stringResource(R.string.library_schedule_mode_simple)) },
        )
        FilterChip(
          selected = schedule.mode == LibraryAdminScheduleMode.Advanced,
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleMode(LibraryAdminScheduleMode.Advanced))
          },
          label = { Text(stringResource(R.string.library_schedule_mode_advanced)) },
        )
      }

      when (schedule.mode) {
        LibraryAdminScheduleMode.Simple ->
          LibraryAdminSimpleScheduleContent(schedule, onEvent)
        LibraryAdminScheduleMode.Advanced ->
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = schedule.advancedCronExpression,
              onValueChange = {
                onEvent(LibraryAdminCreateEvent.UpdateAdvancedScheduleCron(it))
              },
              label = { Text(stringResource(R.string.library_schedule_cron_expression)) },
              placeholder = { Text(stringResource(R.string.library_schedule_cron_hint)) },
              modifier = Modifier.fillMaxWidth(),
              isError = scheduleError != null,
              supportingText = {
                when (val validation = uiState.scheduleValidation) {
                  LibraryAdminScheduleValidationState.Validating ->
                    Text(stringResource(R.string.library_schedule_checking))
                  is LibraryAdminScheduleValidationState.Invalid,
                  is LibraryAdminScheduleValidationState.Unavailable ->
                    Text(
                      text = errorDescription.orEmpty(),
                      modifier = Modifier.semantics {
                        contentDescription = errorDescription.orEmpty()
                      },
                    )
                  else ->
                    if (scheduleError != null) {
                      Text(
                        text = errorDescription.orEmpty(),
                        modifier = Modifier.semantics {
                          contentDescription = errorDescription.orEmpty()
                        },
                      )
                    }
                }
              },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            )
            Button(
              enabled = !uiState.isBusy,
              onClick = { onEvent(LibraryAdminCreateEvent.ValidateSchedule) },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                if (uiState.scheduleValidation is
                    LibraryAdminScheduleValidationState.Validating
                ) {
                  stringResource(R.string.library_schedule_checking)
                } else {
                  stringResource(R.string.library_schedule_validate)
                }
              )
            }
          }
      }

      if (schedule.mode == LibraryAdminScheduleMode.Simple && scheduleError != null) {
        Text(
          text = errorDescription.orEmpty(),
          modifier = Modifier.semantics { contentDescription = errorDescription.orEmpty() },
        )
      }

      if (scheduleError == null &&
          (schedule.mode == LibraryAdminScheduleMode.Simple ||
            uiState.scheduleValidation is LibraryAdminScheduleValidationState.Valid)) {
        ScheduleSummary(schedule.cronExpression, schedule.summary)
      }
    }
  }
}

@Composable
private fun LibraryAdminSimpleScheduleContent(
  schedule: LibraryAdminScheduleDraft,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
  ) {
    OutlinedTextField(
      value = schedule.simple.interval.scheduleLabel(),
      onValueChange = {},
      readOnly = true,
      label = { Text(stringResource(R.string.library_schedule_interval)) },
      modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      LibraryAdminScheduleInterval.entries.forEach { interval ->
        DropdownMenuItem(
          text = { Text(interval.scheduleLabel()) },
          onClick = {
            onEvent(LibraryAdminCreateEvent.SelectScheduleInterval(interval))
            expanded = false
          },
        )
      }
    }
  }

  if (schedule.simple.interval == LibraryAdminScheduleInterval.Custom) {
    Text(stringResource(R.string.library_schedule_weekdays))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LIBRARY_SCHEDULE_WEEKDAY_OPTIONS.forEach { (day, label) ->
        FilterChip(
          selected = day in schedule.simple.weekdays,
          onClick = {
            onEvent(
              LibraryAdminCreateEvent.ToggleScheduleWeekday(
                weekday = day,
                selected = day !in schedule.simple.weekdays,
              )
            )
          },
          label = { Text(stringResource(label)) },
        )
      }
    }
  }

  if (schedule.simple.interval == LibraryAdminScheduleInterval.Custom ||
      schedule.simple.interval == LibraryAdminScheduleInterval.Daily) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(
        value = schedule.simple.hour,
        onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateScheduleHour(it)) },
        label = { Text(stringResource(R.string.library_schedule_hour)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f),
        isError = schedule.simple.hour.toIntOrNull() !in 0..23,
      )
      OutlinedTextField(
        value = schedule.simple.minute,
        onValueChange = { onEvent(LibraryAdminCreateEvent.UpdateScheduleMinute(it)) },
        label = { Text(stringResource(R.string.library_schedule_minute)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f),
        isError = schedule.simple.minute.toIntOrNull() !in 0..59,
      )
    }
  }
}

@Composable
private fun ScheduleSummary(expression: String?, summary: String?) {
  if (expression.isNullOrBlank()) return
  val nextRun = remember(expression) { nextLibraryScheduleRun(expression) }
  Column(
    modifier = Modifier.fillMaxWidth().semantics { contentDescription = expression },
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Text(
      text = summary ?: stringResource(R.string.library_schedule_valid),
      modifier = Modifier.fillMaxWidth(),
    )
    Text(text = expression, modifier = Modifier.fillMaxWidth())
    if (nextRun.isNotBlank()) {
      Text(
        text = stringResource(R.string.library_schedule_next_run, nextRun),
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun LibraryAdminScheduleInterval.scheduleLabel(): String =
  when (this) {
    LibraryAdminScheduleInterval.Custom ->
      stringResource(R.string.library_schedule_interval_custom)
    LibraryAdminScheduleInterval.Daily ->
      stringResource(R.string.library_schedule_interval_daily)
    LibraryAdminScheduleInterval.Every12Hours ->
      stringResource(R.string.library_schedule_interval_every_12_hours)
    LibraryAdminScheduleInterval.Every6Hours ->
      stringResource(R.string.library_schedule_interval_every_6_hours)
    LibraryAdminScheduleInterval.Every2Hours ->
      stringResource(R.string.library_schedule_interval_every_2_hours)
    LibraryAdminScheduleInterval.EveryHour ->
      stringResource(R.string.library_schedule_interval_every_hour)
    LibraryAdminScheduleInterval.Every30Minutes ->
      stringResource(R.string.library_schedule_interval_every_30_minutes)
    LibraryAdminScheduleInterval.Every15Minutes ->
      stringResource(R.string.library_schedule_interval_every_15_minutes)
  }

private val LIBRARY_SCHEDULE_WEEKDAY_OPTIONS =
  listOf(
    0 to R.string.library_schedule_sunday,
    1 to R.string.library_schedule_monday,
    2 to R.string.library_schedule_tuesday,
    3 to R.string.library_schedule_wednesday,
    4 to R.string.library_schedule_thursday,
    5 to R.string.library_schedule_friday,
    6 to R.string.library_schedule_saturday,
  )

@Composable
internal fun LibraryAdminScannerContent(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  Column(
    modifier =
      Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp)
        .focusRequester(focusRequester)
        .focusable(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(stringResource(R.string.library_scanner_heading))
    Text(stringResource(R.string.library_scanner_description))
    uiState.draft.metadataSources.forEachIndexed { index, source ->
      val priority = uiState.draft.metadataPriority(source.id)
      val sourceDescription =
        if (priority == null) source.name
        else stringResource(R.string.library_scanner_source_priority, source.name, priority)
      Row(
        modifier =
          Modifier.fillMaxWidth().semantics { contentDescription = sourceDescription },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(priority?.toString().orEmpty(), modifier = Modifier.width(28.dp))
        MySwitch(
          modifier = Modifier.weight(1f),
          title = source.name,
          checked = source.enabled,
          contentDescription = source.name,
          onCheckedChange = {
            onEvent(LibraryAdminCreateEvent.ToggleMetadataSource(source.id, it))
          },
        )
        TextButton(
          enabled = index > 0,
          onClick = { onEvent(LibraryAdminCreateEvent.MoveMetadataSource(source.id, -1)) },
        ) {
          Text(stringResource(R.string.move_up))
        }
        TextButton(
          enabled = index < uiState.draft.metadataSources.lastIndex,
          onClick = { onEvent(LibraryAdminCreateEvent.MoveMetadataSource(source.id, 1)) },
        ) {
          Text(stringResource(R.string.move_down))
        }
      }
    }
    if (uiState.validation.errors.containsKey(LibraryAdminCreateField.SCANNER_PRECEDENCE)) {
      val scannerErrorDescription = stringResource(R.string.library_scanner_validation)
      Text(
        text = createErrorText(uiState.validation.errors.getValue(LibraryAdminCreateField.SCANNER_PRECEDENCE)),
        modifier = Modifier.semantics { contentDescription = scannerErrorDescription },
      )
    }
  }
}

@Composable
private fun LibraryAdminProviderPicker(
  uiState: LibraryAdminCreateUiState,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  var expanded by remember { mutableStateOf(false) }
  when (val providerState = uiState.providerState) {
    LibraryAdminProviderState.Loading -> {
      val loadingDescription = stringResource(R.string.library_provider_loading)
      Column(
        modifier =
          Modifier.fillMaxWidth()
            .focusRequester(focusRequester)
            .focusable()
            .semantics { contentDescription = loadingDescription },
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        LinearProgressIndicator(Modifier.fillMaxWidth())
        Text(loadingDescription)
      }
    }
    is LibraryAdminProviderState.Failure -> {
      val errorDescription = stringResource(R.string.library_provider_load_failed)
      val retryDescription = stringResource(R.string.retry)
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(errorDescription)
        TextButton(
          onClick = { onEvent(LibraryAdminCreateEvent.RetryProviders) },
          modifier =
            Modifier.fillMaxWidth()
              .focusRequester(focusRequester)
              .semantics { contentDescription = "$errorDescription $retryDescription" },
        ) {
          Text(retryDescription)
        }
      }
    }
    is LibraryAdminProviderState.Success -> {
      ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
      ) {
        val selected = providerState.providers.firstOrNull { it.id == uiState.draft.provider }
        OutlinedTextField(
          modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).menuAnchor(
            ExposedDropdownMenuAnchorType.PrimaryNotEditable
          ),
          readOnly = true,
          value = selected?.name.orEmpty(),
          onValueChange = {},
          label = { Text(stringResource(R.string.library_create_provider)) },
          isError = uiState.validation.errors.containsKey(LibraryAdminCreateField.PROVIDER),
          supportingText =
            uiState.validation.errors[LibraryAdminCreateField.PROVIDER]?.let {
              providerErrors -> { Text(createErrorText(providerErrors)) }
            },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          providerState.providers.forEach { provider ->
            DropdownMenuItem(
              text = { Text(provider.name) },
              onClick = {
                onEvent(LibraryAdminCreateEvent.SelectProvider(provider.id))
                expanded = false
              },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LibraryAdminFilesystemDialog(
  filesystemState: LibraryAdminFilesystemState.Success,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
) {
  AlertDialog(
    onDismissRequest = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) },
    title = { Text(stringResource(R.string.library_filesystem_browser)) },
    text = {
      val currentPathDescription = stringResource(R.string.library_filesystem_current_path)
      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          text = filesystemState.path ?: stringResource(R.string.library_filesystem_root),
          modifier =
            Modifier.semantics {
              contentDescription = currentPathDescription
            },
        )
        if (filesystemState.filesystem.directories.isEmpty()) {
          Text(stringResource(R.string.library_filesystem_empty))
        } else {
          val directoryListDescription =
            stringResource(R.string.library_filesystem_directory_list)
          Column(
            modifier =
              Modifier.fillMaxWidth()
                .heightIn(max = 320.dp)
                .verticalScroll(rememberScrollState())
                .semantics { contentDescription = directoryListDescription },
            verticalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            filesystemState.filesystem.directories.forEach { directory ->
              LibraryAdminDirectoryItem(directory, onEvent)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onEvent(LibraryAdminCreateEvent.CloseFilesystem) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun LibraryAdminDirectoryItem(
  directory: LibraryAdminDirectory,
  onEvent: (LibraryAdminCreateEvent) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(directory.name, modifier = Modifier.weight(1f))
    TextButton(onClick = { onEvent(LibraryAdminCreateEvent.SelectFolder(directory.path)) }) {
      Text(stringResource(R.string.select_folder))
    }
    TextButton(onClick = { onEvent(LibraryAdminCreateEvent.OpenFilesystemPath(directory.path)) }) {
      Text(stringResource(R.string.open))
    }
  }
}

@Composable
private fun createTabLabel(tab: LibraryAdminCreateTab): String =
  when (tab) {
    LibraryAdminCreateTab.DETAILS -> stringResource(R.string.details)
    LibraryAdminCreateTab.SETTINGS -> stringResource(R.string.settings)
    LibraryAdminCreateTab.SCANNER -> stringResource(R.string.scanner)
    LibraryAdminCreateTab.SCHEDULE -> stringResource(R.string.schedule)
  }

@Composable
private fun createErrorText(errors: List<LibraryAdminCreateError>): String {
  val messages = mutableListOf<String>()
  for (error in errors) {
    messages +=
      when (error) {
        LibraryAdminCreateError.NAME_REQUIRED ->
          stringResource(R.string.library_create_error_name_required)
        LibraryAdminCreateError.MEDIA_TYPE_REQUIRED ->
          stringResource(R.string.library_create_error_media_type_required)
        LibraryAdminCreateError.PROVIDER_REQUIRED ->
          stringResource(R.string.library_create_error_provider_required)
        LibraryAdminCreateError.PROVIDER_UNAVAILABLE ->
          stringResource(R.string.library_create_error_provider_unavailable)
        LibraryAdminCreateError.FOLDERS_REQUIRED ->
          stringResource(R.string.library_create_error_folders_required)
        LibraryAdminCreateError.DUPLICATE_FOLDER ->
          stringResource(R.string.library_create_error_duplicate_folder)
        LibraryAdminCreateError.OVERLAPPING_FOLDER ->
          stringResource(R.string.library_create_error_overlapping_folder)
        LibraryAdminCreateError.INVALID_FINISH_THRESHOLD ->
          stringResource(R.string.library_settings_error_finish_threshold)
        LibraryAdminCreateError.SCANNER_PRECEDENCE_REQUIRED ->
          stringResource(R.string.library_scanner_error_source_required)
        LibraryAdminCreateError.SCHEDULE_REQUIRED ->
          stringResource(R.string.library_schedule_error_required)
        LibraryAdminCreateError.SCHEDULE_INVALID ->
          stringResource(R.string.library_schedule_error_invalid)
        LibraryAdminCreateError.SCHEDULE_VALIDATION_UNAVAILABLE ->
          stringResource(R.string.library_schedule_error_unavailable)
      }
  }
  return messages.joinToString(separator = ", ")
}

private fun libraryIconResource(icon: String): Int =
  when (icon) {
    "database" -> R.drawable.library_icon_storage
    "audiobookshelf" -> R.drawable.library_icon_library_music
    "books-1", "books-2", "book-1" -> R.drawable.library_icon_book
    "microphone-1", "microphone-3" -> R.drawable.library_icon_mic
    "radio" -> R.drawable.library_icon_radio
    "podcast", "rss" -> R.drawable.library_icon_rss_feed
    "headphones" -> R.drawable.library_icon_headphones
    "music" -> R.drawable.library_icon_music_note
    "file-picture" -> R.drawable.library_icon_image
    "rocket" -> R.drawable.library_icon_rocket_launch
    "power" -> R.drawable.library_icon_power_settings_new
    "star" -> R.drawable.library_icon_star
    "heart" -> R.drawable.library_icon_favorite
    else -> R.drawable.library_icon_library_music
  }

@ShelfDroidPreview
@Composable
private fun LibraryAdminCreateContentPreview() {
  PreviewWrapper {
    LibraryAdminCreateContent(
      uiState =
        LibraryAdminCreateUiState(
          providerState =
            LibraryAdminProviderState.Success(
              listOf(
                dev.halim.shelfdroid.core.data.screen.libraryadmin.LibraryAdminProvider(
                  "audible",
                  "Audible",
                )
              )
            )
        )
    )
  }
}
