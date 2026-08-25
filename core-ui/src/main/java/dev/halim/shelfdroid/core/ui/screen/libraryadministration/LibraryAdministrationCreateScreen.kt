@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadministration

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
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateError
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateNavigation
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateSubmissionState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateTab
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDirectory
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationDraft
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationMediaType
import dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProviderState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LibraryAdministrationCreateScreen(
  viewModel: LibraryAdministrationCreateViewModel = hiltViewModel(),
  onNavigateBack: () -> Unit = {},
  onCreated: (String) -> Unit = {},
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  BackHandler { viewModel.onEvent(LibraryAdministrationCreateEvent.Back) }

  LaunchedEffect(uiState.navigation) {
    when (val navigation = uiState.navigation) {
      LibraryAdministrationCreateNavigation.Back -> {
        viewModel.onEvent(LibraryAdministrationCreateEvent.ConsumeNavigation)
        onNavigateBack()
      }
      is LibraryAdministrationCreateNavigation.Created -> {
        viewModel.onEvent(LibraryAdministrationCreateEvent.ConsumeNavigation)
        onCreated(navigation.library.id)
      }
      null -> Unit
    }
  }

  LibraryAdministrationCreateContent(uiState, viewModel::onEvent)
}

@Composable
internal fun LibraryAdministrationCreateContent(
  uiState: LibraryAdministrationCreateUiState = LibraryAdministrationCreateUiState(),
  onEvent: (LibraryAdministrationCreateEvent) -> Unit = {},
) {
  val nameFocusRequester = remember { FocusRequester() }
  val providerFocusRequester = remember { FocusRequester() }
  val folderFocusRequester = remember { FocusRequester() }

  LaunchedEffect(uiState.focusField) {
    when (uiState.focusField) {
      LibraryAdministrationCreateField.NAME -> nameFocusRequester.requestFocus()
      LibraryAdministrationCreateField.PROVIDER -> providerFocusRequester.requestFocus()
      LibraryAdministrationCreateField.FOLDERS -> folderFocusRequester.requestFocus()
      else -> Unit
    }
    if (uiState.focusField != null) onEvent(LibraryAdministrationCreateEvent.ConsumeFocus)
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
        LibraryAdministrationCreateTab.DETAILS ->
          LibraryAdministrationDetailsContent(
            uiState = uiState,
            onEvent = onEvent,
            nameFocusRequester = nameFocusRequester,
            providerFocusRequester = providerFocusRequester,
            folderFocusRequester = folderFocusRequester,
          )
        LibraryAdministrationCreateTab.SETTINGS ->
          Text(
            text = stringResource(R.string.library_create_settings_coming_soon),
            modifier = Modifier.padding(16.dp),
          )
        LibraryAdministrationCreateTab.SCANNER ->
          Text(
            text = stringResource(R.string.library_create_scanner_coming_soon),
            modifier = Modifier.padding(16.dp),
          )
      }
    }

    ScrollableTabRow(
      selectedTabIndex = uiState.selectedTab.ordinal,
      modifier = Modifier.fillMaxWidth(),
    ) {
      LibraryAdministrationCreateTab.values().forEach { tab ->
        if (tab != LibraryAdministrationCreateTab.SCANNER ||
          uiState.draft.mediaType != LibraryAdministrationMediaType.PODCAST
        ) {
          Tab(
            selected = uiState.selectedTab == tab,
            onClick = { onEvent(LibraryAdministrationCreateEvent.SelectTab(tab)) },
            text = { Text(createTabLabel(tab)) },
          )
        }
      }
    }

    Button(
      modifier = Modifier.fillMaxWidth().padding(16.dp),
      enabled = !uiState.isSubmitting,
      onClick = { onEvent(LibraryAdministrationCreateEvent.Submit) },
    ) {
      if (uiState.isSubmitting) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(8.dp))
      }
      Text(stringResource(R.string.create_library))
    }

    when (val submission = uiState.submissionState) {
      is LibraryAdministrationCreateSubmissionState.ServerFailure ->
        Text(
          text = stringResource(R.string.library_create_server_failed),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
      is LibraryAdministrationCreateSubmissionState.LocalSyncFailure -> {
        Text(
          text = stringResource(R.string.library_create_local_sync_failed),
          modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        )
        TextButton(
          onClick = { onEvent(LibraryAdministrationCreateEvent.RetryLocalSynchronization) },
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
      onDismissRequest = { onEvent(LibraryAdministrationCreateEvent.CancelDiscard) },
      title = { Text(stringResource(R.string.library_create_discard_title)) },
      text = { Text(stringResource(R.string.library_create_discard_message)) },
      confirmButton = {
        TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.ConfirmDiscard) }) {
          Text(stringResource(R.string.library_create_discard))
        }
      },
      dismissButton = {
        TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.CancelDiscard) }) {
          Text(stringResource(R.string.cancel))
        }
      },
    )
  }

  when (val filesystem = uiState.filesystemState) {
    is LibraryAdministrationFilesystemState.Success ->
      LibraryAdministrationFilesystemDialog(filesystem, onEvent)
    is LibraryAdministrationFilesystemState.Failure ->
      AlertDialog(
        onDismissRequest = { onEvent(LibraryAdministrationCreateEvent.CloseFilesystem) },
        title = { Text(stringResource(R.string.library_filesystem_browser)) },
        text = {
          Text(stringResource(R.string.library_filesystem_load_failed))
        },
        confirmButton = {
          TextButton(
            onClick = {
              onEvent(
                LibraryAdministrationCreateEvent.OpenFilesystemPath(filesystem.path.orEmpty())
              )
            }
          ) {
            Text(stringResource(R.string.retry))
          }
        },
        dismissButton = {
          TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.CloseFilesystem) }) {
            Text(stringResource(R.string.cancel))
          }
        },
      )
    is LibraryAdministrationFilesystemState.Loading ->
      AlertDialog(
        onDismissRequest = { onEvent(LibraryAdministrationCreateEvent.CloseFilesystem) },
        title = { Text(stringResource(R.string.library_filesystem_browser)) },
        text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
        confirmButton = {},
      )
    LibraryAdministrationFilesystemState.Closed -> Unit
  }
}

@Composable
internal fun LibraryAdministrationDetailsContent(
  uiState: LibraryAdministrationCreateUiState,
  onEvent: (LibraryAdministrationCreateEvent) -> Unit,
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
        selected = uiState.draft.mediaType == LibraryAdministrationMediaType.BOOK,
        onClick = {
          onEvent(
            LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.BOOK)
          )
        },
        label = { Text(stringResource(R.string.book_library)) },
      )
      FilterChip(
        selected = uiState.draft.mediaType == LibraryAdministrationMediaType.PODCAST,
        onClick = {
          onEvent(
            LibraryAdministrationCreateEvent.SelectMediaType(LibraryAdministrationMediaType.PODCAST)
          )
        },
        label = { Text(stringResource(R.string.podcast_library)) },
      )
    }

    MyOutlinedTextField(
      modifier = Modifier.focusRequester(nameFocusRequester),
      value = uiState.draft.name,
      onValueChange = { onEvent(LibraryAdministrationCreateEvent.UpdateName(it)) },
      label = stringResource(R.string.library_create_name),
      supportingText =
        errors[LibraryAdministrationCreateField.NAME]?.let { nameErrors ->
          createErrorText(nameErrors)
        },
      isError = errors.containsKey(LibraryAdministrationCreateField.NAME),
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
    )

    Text(stringResource(R.string.library_create_icon))
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      LibraryAdministrationDraft.ICON_IDS.forEach { icon ->
        FilterChip(
          selected = uiState.draft.icon == icon,
          onClick = { onEvent(LibraryAdministrationCreateEvent.SelectIcon(icon)) },
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

    LibraryAdministrationProviderPicker(uiState, onEvent, providerFocusRequester)

    Text(stringResource(R.string.library_create_folders))
    uiState.draft.folders.forEach { folder ->
      Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(folder, modifier = Modifier.weight(1f))
        IconButton(onClick = { onEvent(LibraryAdministrationCreateEvent.RemoveFolder(folder)) }) {
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
        onValueChange = { onEvent(LibraryAdministrationCreateEvent.UpdateManualFolder(it)) },
        label = { Text(stringResource(R.string.library_folder_path)) },
        isError = errors.containsKey(LibraryAdministrationCreateField.FOLDERS),
        supportingText =
          errors[LibraryAdministrationCreateField.FOLDERS]?.let { folderErrors ->
            { Text(createErrorText(folderErrors)) }
          },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
      )
      Spacer(Modifier.width(8.dp))
      TextButton(
        onClick = { onEvent(LibraryAdministrationCreateEvent.AddManualFolder) },
        modifier = Modifier.padding(top = 4.dp),
      ) {
        Text(stringResource(R.string.add))
      }
    }
    TextButton(
      onClick = { onEvent(LibraryAdministrationCreateEvent.OpenFilesystem) },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Text(stringResource(R.string.library_browse_server_folders))
    }
  }
}

@Composable
private fun LibraryAdministrationProviderPicker(
  uiState: LibraryAdministrationCreateUiState,
  onEvent: (LibraryAdministrationCreateEvent) -> Unit,
  focusRequester: FocusRequester,
) {
  var expanded by remember { mutableStateOf(false) }
  when (val providerState = uiState.providerState) {
    LibraryAdministrationProviderState.Loading -> {
      LinearProgressIndicator(Modifier.fillMaxWidth())
      Text(stringResource(R.string.library_provider_loading))
    }
    is LibraryAdministrationProviderState.Failure -> {
      Text(
        stringResource(R.string.library_provider_load_failed),
        modifier = Modifier.fillMaxWidth(),
      )
      TextButton(
        onClick = { onEvent(LibraryAdministrationCreateEvent.RetryProviders) },
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(stringResource(R.string.retry))
      }
    }
    is LibraryAdministrationProviderState.Success -> {
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
          isError = uiState.validation.errors.containsKey(LibraryAdministrationCreateField.PROVIDER),
          supportingText =
            uiState.validation.errors[LibraryAdministrationCreateField.PROVIDER]?.let {
              providerErrors -> { Text(createErrorText(providerErrors)) }
            },
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
          providerState.providers.forEach { provider ->
            DropdownMenuItem(
              text = { Text(provider.name) },
              onClick = {
                onEvent(LibraryAdministrationCreateEvent.SelectProvider(provider.id))
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
private fun LibraryAdministrationFilesystemDialog(
  filesystemState: LibraryAdministrationFilesystemState.Success,
  onEvent: (LibraryAdministrationCreateEvent) -> Unit,
) {
  AlertDialog(
    onDismissRequest = { onEvent(LibraryAdministrationCreateEvent.CloseFilesystem) },
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
              LibraryAdministrationDirectoryItem(directory, onEvent)
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.CloseFilesystem) }) {
        Text(stringResource(R.string.cancel))
      }
    },
  )
}

@Composable
private fun LibraryAdministrationDirectoryItem(
  directory: LibraryAdministrationDirectory,
  onEvent: (LibraryAdministrationCreateEvent) -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    Text(directory.name, modifier = Modifier.weight(1f))
    TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.SelectFolder(directory.path)) }) {
      Text(stringResource(R.string.select_folder))
    }
    TextButton(onClick = { onEvent(LibraryAdministrationCreateEvent.OpenFilesystemPath(directory.path)) }) {
      Text(stringResource(R.string.open))
    }
  }
}

@Composable
private fun createTabLabel(tab: LibraryAdministrationCreateTab): String =
  when (tab) {
    LibraryAdministrationCreateTab.DETAILS -> stringResource(R.string.details)
    LibraryAdministrationCreateTab.SETTINGS -> stringResource(R.string.settings)
    LibraryAdministrationCreateTab.SCANNER -> stringResource(R.string.scanner)
  }

@Composable
private fun createErrorText(errors: List<LibraryAdministrationCreateError>): String {
  val messages = mutableListOf<String>()
  for (error in errors) {
    messages +=
      when (error) {
        LibraryAdministrationCreateError.NAME_REQUIRED ->
          stringResource(R.string.library_create_error_name_required)
        LibraryAdministrationCreateError.MEDIA_TYPE_REQUIRED ->
          stringResource(R.string.library_create_error_media_type_required)
        LibraryAdministrationCreateError.PROVIDER_REQUIRED ->
          stringResource(R.string.library_create_error_provider_required)
        LibraryAdministrationCreateError.PROVIDER_UNAVAILABLE ->
          stringResource(R.string.library_create_error_provider_unavailable)
        LibraryAdministrationCreateError.FOLDERS_REQUIRED ->
          stringResource(R.string.library_create_error_folders_required)
        LibraryAdministrationCreateError.DUPLICATE_FOLDER ->
          stringResource(R.string.library_create_error_duplicate_folder)
        LibraryAdministrationCreateError.OVERLAPPING_FOLDER ->
          stringResource(R.string.library_create_error_overlapping_folder)
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
private fun LibraryAdministrationCreateContentPreview() {
  PreviewWrapper {
    LibraryAdministrationCreateContent(
      uiState =
        LibraryAdministrationCreateUiState(
          providerState =
            LibraryAdministrationProviderState.Success(
              listOf(
                dev.halim.shelfdroid.core.data.screen.libraryadministration.LibraryAdministrationProvider(
                  "audible",
                  "Audible",
                )
              )
            )
        )
    )
  }
}
