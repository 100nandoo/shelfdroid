@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.CoverSearchState
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.SectionCard
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun CoverTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val context = LocalContext.current
  val coverSearch = uiState.coverSearch
  var url by remember { mutableStateOf("") }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var wasLoadingResults by remember { mutableStateOf(false) }
  val resultsBringIntoViewRequester = remember { BringIntoViewRequester() }

  val pickLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        onEvent(EditItemEvent.UploadCover(uri, context.contentResolver))
      }
    }

  LaunchedEffect(coverSearch.state, coverSearch.results) {
    val isLoadingResults = coverSearch.state.isLoading()
    if (wasLoadingResults && !isLoadingResults && coverSearch.results.isNotEmpty()) {
      resultsBringIntoViewRequester.bringIntoView()
    }
    wasLoadingResults = isLoadingResults
  }

  MyAlertDialog(
    showDialog = showDeleteDialog,
    title = stringResource(R.string.edit_item_remove_cover),
    text = stringResource(R.string.edit_item_remove_cover_confirm),
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      showDeleteDialog = false
      onEvent(EditItemEvent.DeleteCover)
    },
    onDismiss = { showDeleteDialog = false },
  )

  Column(
    modifier =
      Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(20.dp),
  ) {
    CoverPreviewSection(
      coverUrl = uiState.coverUrl,
      isCoverWorking = uiState.isCoverWorking,
      onPickFromDevice = { pickLauncher.launch("image/*") },
      onDeleteCover = { showDeleteDialog = true },
    )

    CoverUrlSection(
      url = url,
      isCoverWorking = uiState.isCoverWorking,
      onUrlChange = { url = it },
      onFetchCover = {
        if (url.isNotBlank()) {
          onEvent(EditItemEvent.SetCoverUrl(url))
          url = ""
        }
      },
    )

    HorizontalDivider()

    CoverSearchSection(
      coverSearch = coverSearch,
      onTitleChange = { onEvent(EditItemEvent.UpdateCoverSearchTitle(it)) },
      onAuthorChange = { onEvent(EditItemEvent.UpdateCoverSearchAuthor(it)) },
      onProviderChange = { onEvent(EditItemEvent.UpdateCoverSearchProvider(it)) },
      onRunSearch = { onEvent(EditItemEvent.RunCoverSearch) },
    )

    if (coverSearch.results.isNotEmpty()) {
      CoverResultsSection(
        modifier = Modifier.fillMaxWidth().bringIntoViewRequester(resultsBringIntoViewRequester),
        results = coverSearch.results,
        isCoverWorking = uiState.isCoverWorking,
        onSelectCover = { onEvent(EditItemEvent.SetCoverUrl(it)) },
      )
    }
  }
}

@Composable
private fun CoverPreviewSection(
  coverUrl: String,
  isCoverWorking: Boolean,
  onPickFromDevice: () -> Unit,
  onDeleteCover: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
      CoverNoAnimation(modifier = Modifier.fillMaxSize(), coverUrl = coverUrl, showFallback = true)
      if (isCoverWorking) {
        CircularProgressIndicator()
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(onClick = onPickFromDevice, enabled = !isCoverWorking) {
        Text(stringResource(R.string.edit_item_upload_cover_from_device))
      }

      if (coverUrl.isNotBlank()) {
        TextButton(onClick = onDeleteCover, enabled = !isCoverWorking) {
          Text(stringResource(R.string.edit_item_remove_cover))
        }
      }
    }
  }
}

@Composable
private fun CoverUrlSection(
  url: String,
  isCoverWorking: Boolean,
  onUrlChange: (String) -> Unit,
  onFetchCover: () -> Unit,
) {
  SectionCard {
    Text(
      stringResource(R.string.edit_item_use_image_url),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      stringResource(R.string.edit_item_use_image_url_help),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
      value = url,
      onValueChange = onUrlChange,
      label = { Text(stringResource(R.string.edit_item_image_url)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Button(onClick = onFetchCover, enabled = !isCoverWorking && url.isNotBlank()) {
      Text(stringResource(R.string.edit_item_fetch_cover))
    }
  }
}

@Composable
private fun CoverSearchSection(
  coverSearch: CoverSearchState,
  onTitleChange: (String) -> Unit,
  onAuthorChange: (String) -> Unit,
  onProviderChange: (String) -> Unit,
  onRunSearch: () -> Unit,
) {
  var providerExpanded by remember { mutableStateOf(false) }
  val providerText =
    coverSearch.providers.find { it.value == coverSearch.provider }?.text ?: coverSearch.provider

  SectionCard {
    Text(
      stringResource(R.string.edit_item_search_for_cover),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      stringResource(R.string.edit_item_search_for_cover_help),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    OutlinedTextField(
      value = coverSearch.title,
      onValueChange = onTitleChange,
      label = { Text(stringResource(R.string.edit_item_title)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = coverSearch.author,
      onValueChange = onAuthorChange,
      label = { Text(stringResource(R.string.author)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )

    ExposedDropdownMenuBox(
      expanded = providerExpanded,
      onExpandedChange = { providerExpanded = it },
    ) {
      OutlinedTextField(
        value = providerText,
        onValueChange = {},
        readOnly = true,
        label = { Text(stringResource(R.string.edit_item_provider)) },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
        modifier =
          Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
      )
      ExposedDropdownMenu(
        expanded = providerExpanded,
        onDismissRequest = { providerExpanded = false },
      ) {
        coverSearch.providers.forEach { provider ->
          DropdownMenuItem(
            text = { Text(provider.text) },
            onClick = {
              onProviderChange(provider.value)
              providerExpanded = false
            },
          )
        }
      }
    }

    Button(
      onClick = onRunSearch,
      enabled = !coverSearch.state.isLoading() && coverSearch.title.isNotBlank(),
    ) {
      Text(stringResource(R.string.search))
    }

    if (coverSearch.state.isLoading()) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
          stringResource(R.string.edit_item_searching_for_covers),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun CoverResultsSection(
  modifier: Modifier = Modifier,
  results: List<String>,
  isCoverWorking: Boolean,
  onSelectCover: (String) -> Unit,
) {
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text(
      stringResource(R.string.edit_item_cover_results),
      style = MaterialTheme.typography.titleMedium,
    )
    Text(
      stringResource(R.string.edit_item_cover_results_help),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val columns = 3
      val spacing = 12.dp
      val rowCount = (results.size + columns - 1) / columns
      val itemSize = (maxWidth - spacing * (columns - 1)) / columns
      val gridHeight = itemSize * rowCount + spacing * (rowCount - 1).coerceAtLeast(0)

      LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxWidth().height(gridHeight),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
      ) {
        items(results) { coverUrl ->
          Surface(
            modifier =
              Modifier.fillMaxWidth().aspectRatio(1f).clickable(enabled = !isCoverWorking) {
                onSelectCover(coverUrl)
              },
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 1.dp,
          ) {
            CoverNoAnimation(
              modifier = Modifier.fillMaxSize(),
              coverUrl = coverUrl,
              showFallback = true,
            )
          }
        }
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun CoverTabPreview() {
  PreviewWrapper { CoverTab(uiState = Defaults.EDIT_ITEM_UI_STATE, onEvent = {}) }
}
