package dev.halim.shelfdroid.core.ui.screen.serversettings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.serversettings.ServerSettingsApiState
import dev.halim.shelfdroid.core.data.screen.serversettings.ServerSettingsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.LabelPosition
import dev.halim.shelfdroid.core.ui.components.MySwitch
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen

@Composable
fun ServerSettingsScreen(
  viewModel: ServerSettingsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  HandleServerSettingsSnackbar(uiState, snackbarHostState)
  ServerSettingsContent(uiState) { event -> viewModel.onEvent(event) }
}

@Composable
private fun ServerSettingsContent(
  uiState: ServerSettingsUiState = ServerSettingsUiState(),
  onEvent: (ServerSettingsEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize()) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is ServerSettingsApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
    }

    Column(
      modifier =
        Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())
    ) {
      Spacer(modifier = Modifier.height(16.dp))
      CacheSection(onEvent)
      Spacer(modifier = Modifier.height(16.dp))
      DisplaySection(uiState, onEvent)
      Spacer(modifier = Modifier.height(16.dp))
      WebClientSection(uiState, onEvent)
      Spacer(modifier = Modifier.height(16.dp))
      ScannerSection(uiState, onEvent)
      Spacer(modifier = Modifier.height(16.dp))
      GeneralSection(uiState, onEvent)
      Spacer(modifier = Modifier.height(16.dp))
    }

    AnimatedVisibility(visible = uiState.hasChanges) {
      Button(
        onClick = { onEvent(ServerSettingsEvent.Save) },
        modifier = Modifier.fillMaxWidth().padding(16.dp),
      ) {
        Text(stringResource(R.string.save))
      }
    }
  }
}

@Composable
private fun GeneralSection(uiState: ServerSettingsUiState, onEvent: (ServerSettingsEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.general))
  val startPadding = Modifier.padding(start = 8.dp)
  val current = uiState.currentState
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.store_covers_with_item),
    checked = current.storeCoverWithItem,
    contentDescription = stringResource(R.string.store_covers_with_item),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(storeCoverWithItem = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.store_metadata_with_item),
    checked = current.storeMetadataWithItem,
    contentDescription = stringResource(R.string.store_metadata_with_item),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(storeMetadataWithItem = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.ignore_prefixes_when_sorting),
    checked = current.sortingIgnorePrefix,
    contentDescription = stringResource(R.string.ignore_prefixes_when_sorting),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(sortingIgnorePrefix = value))
        }
      )
    },
  )
}

@Composable
private fun ScannerSection(uiState: ServerSettingsUiState, onEvent: (ServerSettingsEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.scanner))
  val startPadding = Modifier.padding(start = 8.dp)
  val current = uiState.currentState
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.parse_subtitles),
    checked = current.scannerParseSubtitle,
    contentDescription = stringResource(R.string.parse_subtitles),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(scannerParseSubtitle = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.find_covers),
    checked = current.scannerFindCovers,
    contentDescription = stringResource(R.string.find_covers),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(scannerFindCovers = value))
        }
      )
    },
  )

  AnimatedVisibility(visible = current.scannerFindCovers) {
    val coverProviderOptions = uiState.coverProviders.map { it.text }
    val currentCoverProviderText =
      uiState.coverProviders.find { it.value == current.scannerCoverProvider }?.text
        ?: current.scannerCoverProvider
    if (coverProviderOptions.isNotEmpty()) {
      ChipDropdownMenu(
        modifier = startPadding,
        label = stringResource(R.string.cover_provider),
        options = coverProviderOptions,
        initialValue = currentCoverProviderText,
        onClick = { selectedText ->
          val provider = uiState.coverProviders.find { it.text == selectedText }?.value ?: ""
          onEvent(
            ServerSettingsEvent.UpdateUiState {
              it.copy(currentState = it.currentState.copy(scannerCoverProvider = provider))
            }
          )
        },
      )
    }
  }

  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.prefer_matched_metadata),
    checked = current.scannerPreferMatchedMetadata,
    contentDescription = stringResource(R.string.prefer_matched_metadata),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(scannerPreferMatchedMetadata = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.watch_for_changes),
    checked = current.watchForChanges,
    contentDescription = stringResource(R.string.watch_for_changes),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(watchForChanges = value))
        }
      )
    },
  )
}

@Composable
private fun WebClientSection(
  uiState: ServerSettingsUiState,
  onEvent: (ServerSettingsEvent) -> Unit,
) {
  TextTitleMedium(text = stringResource(R.string.web_client))
  val startPadding = Modifier.padding(start = 8.dp)
  val current = uiState.currentState
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.chromecast_support),
    checked = current.chromecastEnabled,
    contentDescription = stringResource(R.string.chromecast_support),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(chromecastEnabled = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.allow_iframe),
    checked = current.allowIframe,
    contentDescription = stringResource(R.string.allow_iframe),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(allowIframe = value))
        }
      )
    },
  )
}

@Composable
private fun DisplaySection(uiState: ServerSettingsUiState, onEvent: (ServerSettingsEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.display))
  val startPadding = Modifier.padding(start = 8.dp)
  val current = uiState.currentState
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.home_bookshelf_view),
    checked = current.homeBookshelfView,
    contentDescription = stringResource(R.string.home_bookshelf_view),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(homeBookshelfView = value))
        }
      )
    },
  )
  MySwitch(
    modifier = startPadding,
    title = stringResource(R.string.library_bookshelf_view),
    checked = current.bookshelfView,
    contentDescription = stringResource(R.string.library_bookshelf_view),
    onCheckedChange = { value ->
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(bookshelfView = value))
        }
      )
    },
  )

  Spacer(modifier = Modifier.height(8.dp))

  Row(modifier = startPadding.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    ChipDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.date_format),
      labelPosition = LabelPosition.Top,
      options = DATE_FORMATS,
      initialValue = current.dateFormat,
      onClick = { value ->
        onEvent(
          ServerSettingsEvent.UpdateUiState {
            it.copy(currentState = it.currentState.copy(dateFormat = value))
          }
        )
      },
    )
    Spacer(Modifier.width(8.dp))
    ChipDropdownMenu(
      label = stringResource(R.string.time_format),
      labelPosition = LabelPosition.Top,
      options = TIME_FORMATS.map { it.second },
      initialValue =
        TIME_FORMATS.find { it.first == current.timeFormat }?.second ?: current.timeFormat,
      onClick = { selectedDisplay ->
        val format = TIME_FORMATS.find { it.second == selectedDisplay }?.first ?: selectedDisplay
        onEvent(
          ServerSettingsEvent.UpdateUiState {
            it.copy(currentState = it.currentState.copy(timeFormat = format))
          }
        )
      },
    )
  }

  Spacer(modifier = Modifier.height(8.dp))

  ChipDropdownMenu(
    modifier = startPadding,
    label = stringResource(R.string.default_language),
    labelPosition = LabelPosition.Top,
    options = LANGUAGES.map { it.second },
    initialValue = LANGUAGES.find { it.first == current.language }?.second ?: current.language,
    onClick = { selectedDisplay ->
      val code = LANGUAGES.find { it.second == selectedDisplay }?.first ?: selectedDisplay
      onEvent(
        ServerSettingsEvent.UpdateUiState {
          it.copy(currentState = it.currentState.copy(language = code))
        }
      )
    },
  )
}

@Composable
private fun CacheSection(onEvent: (ServerSettingsEvent) -> Unit) {
  TextTitleMedium(text = stringResource(R.string.cache))
  Row(modifier = Modifier.padding(start = 8.dp)) {
    TextButton(onClick = { onEvent(ServerSettingsEvent.PurgeCache) }) {
      Text(stringResource(R.string.purge_all_cache))
    }
    Spacer(modifier = Modifier.width(8.dp))
    TextButton(onClick = { onEvent(ServerSettingsEvent.PurgeItemsCache) }) {
      Text(stringResource(R.string.purge_items_cache))
    }
  }
}

@Composable
private fun HandleServerSettingsSnackbar(
  uiState: ServerSettingsUiState,
  snackbarHostState: SnackbarHostState,
) {
  val settingsSuccess = stringResource(R.string.settings_saved)
  val settingsError = stringResource(R.string.settings_save_failed)
  val purgeCacheSuccess = stringResource(R.string.cache_purged)
  val purgeCacheError = stringResource(R.string.purge_cache_failed)
  val purgeItemsCacheSuccess = stringResource(R.string.items_cache_purged)
  val purgeItemsCacheError = stringResource(R.string.purge_items_cache_failed)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      ServerSettingsApiState.SettingsSuccess ->
        snackbarHostState.showSuccessSnackbar(settingsSuccess)
      is ServerSettingsApiState.SettingsFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: settingsError)
      ServerSettingsApiState.PurgeCacheSuccess ->
        snackbarHostState.showSuccessSnackbar(purgeCacheSuccess)
      is ServerSettingsApiState.PurgeCacheFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: purgeCacheError)
      ServerSettingsApiState.PurgeItemsCacheSuccess ->
        snackbarHostState.showSuccessSnackbar(purgeItemsCacheSuccess)
      is ServerSettingsApiState.PurgeItemsCacheFailure ->
        snackbarHostState.showErrorSnackbar(state.message ?: purgeItemsCacheError)
      else -> Unit
    }
  }
}

@ShelfDroidPreview
@Composable
fun ServerSettingsScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { ServerSettingsContent() }
}
