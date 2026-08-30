@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProvider
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProviderState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminDetailsTab
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminFilesystemDialog
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminScannerTab
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminScheduleTab
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs.LibraryAdminSettingsTab

@Composable
fun LibraryAdminCreateScreen(
  libraryId: String? = null,
  viewModel: LibraryAdminCreateViewModel =
    hiltViewModel<LibraryAdminCreateViewModel, LibraryAdminCreateViewModel.Factory> { factory ->
      factory.create(libraryId)
    },
  onNavigateBack: () -> Unit = {},
  onSaved: (String) -> Unit = {},
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
        onSaved(navigation.library.id)
      }

      is LibraryAdminCreateNavigation.Updated -> {
        viewModel.onEvent(LibraryAdminCreateEvent.ConsumeNavigation)
        onSaved(navigation.library.id)
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
    if (uiState.isLoadingLibrary) {
      LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    if (uiState.libraryLoadFailed) {
      Text(
        text = stringResource(R.string.library_edit_load_failed),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      )
      TextButton(
        onClick = { onEvent(LibraryAdminCreateEvent.Load) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.retry))
      }
    }
    Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
      Text(
        text =
          stringResource(if (uiState.isEdit) R.string.edit_library else R.string.create_library),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
      )
      when (uiState.selectedTab) {
        LibraryAdminCreateTab.DETAILS ->
          LibraryAdminDetailsTab(
            uiState = uiState,
            onEvent = onEvent,
            nameFocusRequester = nameFocusRequester,
            providerFocusRequester = providerFocusRequester,
            folderFocusRequester = folderFocusRequester,
          )

        LibraryAdminCreateTab.SETTINGS ->
          LibraryAdminSettingsTab(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = settingsFocusRequester,
          )

        LibraryAdminCreateTab.SCANNER ->
          LibraryAdminScannerTab(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = scannerFocusRequester,
          )

        LibraryAdminCreateTab.SCHEDULE ->
          LibraryAdminScheduleTab(
            uiState = uiState,
            onEvent = onEvent,
            focusRequester = scheduleFocusRequester,
          )
      }
    }

    val visibleTabs =
      LibraryAdminCreateTab.entries.filter {
        it != LibraryAdminCreateTab.SCANNER || uiState.draft.mediaType != MediaType.PODCAST
      }
    PrimaryScrollableTabRow(
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
      enabled =
        !uiState.isBusy && !uiState.libraryLoadFailed && (!uiState.isEdit || uiState.isDirty),
      onClick = { onEvent(LibraryAdminCreateEvent.Submit) },
    ) {
      if (uiState.isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
      }
      Text(stringResource(if (uiState.isEdit) R.string.save else R.string.create_library))
    }

    when (val submission = uiState.submissionState) {
      is LibraryAdminCreateSubmissionState.ServerFailure ->
        Text(
          text =
            stringResource(
              if (uiState.isEdit) {
                R.string.library_edit_server_failed
              } else {
                R.string.library_create_server_failed
              }
            ),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )

      is LibraryAdminCreateSubmissionState.LocalSyncFailure -> {
        Text(
          text =
            stringResource(
              if (uiState.isEdit) {
                R.string.library_edit_local_sync_failed
              } else {
                R.string.library_create_local_sync_failed
              }
            ),
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
    is LibraryAdminFilesystemState.Success -> LibraryAdminFilesystemDialog(filesystem, onEvent)
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
              onEvent(LibraryAdminCreateEvent.OpenFilesystemPath(filesystem.path.orEmpty()))
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
private fun createTabLabel(tab: LibraryAdminCreateTab): String =
  when (tab) {
    LibraryAdminCreateTab.DETAILS -> stringResource(R.string.details)
    LibraryAdminCreateTab.SETTINGS -> stringResource(R.string.settings)
    LibraryAdminCreateTab.SCANNER -> stringResource(R.string.scanner)
    LibraryAdminCreateTab.SCHEDULE -> stringResource(R.string.schedule)
  }

@Composable
internal fun createErrorText(errors: List<LibraryAdminCreateError>): String {
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
                LibraryAdminProvider(
                  "audible",
                  "Audible",
                )
              )
            )
        )
    )
  }
}
