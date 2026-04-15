@file:OptIn(
  androidx.compose.material3.ExperimentalMaterial3Api::class,
  androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun CoverTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val context = LocalContext.current
  var url by remember { mutableStateOf("") }

  val pickLauncher =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
      if (uri != null) {
        onEvent(EditItemEvent.UploadCover(uri, context.contentResolver))
      }
    }

  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
      CoverNoAnimation(modifier = Modifier.fillMaxWidth(), coverUrl = uiState.coverUrl)
      if (uiState.isCoverWorking) {
        CircularProgressIndicator()
      }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Button(onClick = { pickLauncher.launch("image/*") }, enabled = !uiState.isCoverWorking) {
        Text(stringResource(R.string.edit_item_upload_cover))
      }
      OutlinedButton(
        onClick = { onEvent(EditItemEvent.DeleteCover) },
        enabled = !uiState.isCoverWorking,
      ) {
        Text(stringResource(R.string.delete))
      }
    }

    OutlinedTextField(
      value = url,
      onValueChange = { url = it },
      label = { Text(stringResource(R.string.edit_item_image_url_from_web)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Button(
      onClick = {
        if (url.isNotBlank()) {
          onEvent(EditItemEvent.SetCoverUrl(url))
          url = ""
        }
      },
      enabled = !uiState.isCoverWorking && url.isNotBlank(),
    ) {
      Text(stringResource(R.string.submit))
    }

    HorizontalDivider()
    Text(
      stringResource(R.string.edit_item_search_for_cover),
      style = MaterialTheme.typography.titleMedium,
    )

    val coverSearch = uiState.coverSearch
    var providerExpanded by remember { mutableStateOf(false) }
    val providerText =
      coverSearch.providers.find { it.value == coverSearch.provider }?.text ?: coverSearch.provider
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
              onEvent(EditItemEvent.UpdateCoverSearchProvider(provider.value))
              providerExpanded = false
            },
          )
        }
      }
    }
    OutlinedTextField(
      value = coverSearch.title,
      onValueChange = { onEvent(EditItemEvent.UpdateCoverSearchTitle(it)) },
      label = { Text(stringResource(R.string.search)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    OutlinedTextField(
      value = coverSearch.author,
      onValueChange = { onEvent(EditItemEvent.UpdateCoverSearchAuthor(it)) },
      label = { Text(stringResource(R.string.author)) },
      modifier = Modifier.fillMaxWidth(),
      singleLine = true,
    )
    Button(
      onClick = { onEvent(EditItemEvent.RunCoverSearch) },
      enabled = !coverSearch.state.isLoading() && coverSearch.title.isNotBlank(),
    ) {
      Text(stringResource(R.string.search))
    }

    if (coverSearch.state.isLoading()) {
      LinearProgressIndicator()
    }

    if (coverSearch.results.isNotEmpty()) {
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        coverSearch.results.forEach { coverUrl ->
          CoverNoAnimation(
            modifier =
              Modifier.size(120.dp).clickable { onEvent(EditItemEvent.SetCoverUrl(coverUrl)) },
            coverUrl = coverUrl,
          )
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
