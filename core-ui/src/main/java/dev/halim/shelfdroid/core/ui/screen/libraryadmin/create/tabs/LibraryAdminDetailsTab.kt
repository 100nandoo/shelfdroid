@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.tabs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.MediaType
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateField
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminCreateUiState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDirectory
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminDraft
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminFilesystemState
import dev.halim.shelfdroid.core.data.screen.libraryadmin.create.LibraryAdminProviderState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.LibraryAdminCreateEvent
import dev.halim.shelfdroid.core.ui.screen.libraryadmin.create.createErrorText

@Composable
internal fun LibraryAdminDetailsTab(
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
    val bookLabel = stringResource(R.string.book_library)
    val podcastLabel = stringResource(R.string.podcast_library)
    MySegmentedButton(
      options = listOf(MediaType.BOOK, MediaType.PODCAST),
      label = stringResource(R.string.library_create_media_type),
      selectedValue = uiState.draft.mediaType,
      optionLabel = { mediaType ->
        if (mediaType == MediaType.BOOK) bookLabel else podcastLabel
      },
      onClick = { onEvent(LibraryAdminCreateEvent.SelectMediaType(it)) },
    )

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
          label = {
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
          Modifier.fillMaxWidth().focusRequester(focusRequester).focusable().semantics {
            contentDescription = loadingDescription
          },
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
            Modifier.fillMaxWidth().focusRequester(focusRequester).semantics {
              contentDescription = "$errorDescription $retryDescription"
            },
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
          modifier =
            Modifier.fillMaxWidth()
              .focusRequester(focusRequester)
              .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
          readOnly = true,
          value = selected?.name.orEmpty(),
          onValueChange = {},
          label = { Text(stringResource(R.string.library_create_provider)) },
          isError = uiState.validation.errors.containsKey(LibraryAdminCreateField.PROVIDER),
          supportingText =
            uiState.validation.errors[LibraryAdminCreateField.PROVIDER]?.let { providerErrors ->
              { Text(createErrorText(providerErrors)) }
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
internal fun LibraryAdminFilesystemDialog(
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
          modifier = Modifier.semantics { contentDescription = currentPathDescription },
        )
        if (filesystemState.filesystem.directories.isEmpty()) {
          Text(stringResource(R.string.library_filesystem_empty))
        } else {
          val directoryListDescription = stringResource(R.string.library_filesystem_directory_list)
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

internal fun libraryIconResource(icon: String): Int =
  when (icon) {
    "database" -> R.drawable.library_icon_storage
    "audiobookshelf" -> R.drawable.library_icon_library_music
    "books-1" -> R.drawable.library_icon_book
    "books-2" -> R.drawable.library_icon_books_2
    "book-1" -> R.drawable.library_icon_book_1

    "microphone-1" -> R.drawable.library_icon_mic
    "microphone-3" -> R.drawable.library_icon_microphone_3

    "radio" -> R.drawable.library_icon_radio
    "podcast" -> R.drawable.library_icon_podcast
    "rss" -> R.drawable.library_icon_rss_feed
    "headphones" -> R.drawable.library_icon_headphones
    "music" -> R.drawable.library_icon_music_note
    "file-picture" -> R.drawable.library_icon_image
    "rocket" -> R.drawable.library_icon_rocket_launch
    "power" -> R.drawable.library_icon_power_settings_new
    "star" -> R.drawable.library_icon_star
    "heart" -> R.drawable.library_icon_favorite
    else -> R.drawable.library_icon_library_music
  }
