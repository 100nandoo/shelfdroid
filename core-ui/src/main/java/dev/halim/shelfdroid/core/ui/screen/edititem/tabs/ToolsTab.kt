package dev.halim.shelfdroid.core.ui.screen.edititem.tabs

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import dev.halim.shelfdroid.core.data.screen.edititem.EditItemUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.edititem.EditItemEvent

@Composable
fun ToolsTab(uiState: EditItemUiState, onEvent: (EditItemEvent) -> Unit) {
  val context = LocalContext.current
  val openManager =
    remember(context, uiState.webBaseUrl, uiState.itemId) {
      { tool: String ->
        val url =
          "${uiState.webBaseUrl}/audiobookshelf/audiobook/${uiState.itemId}/manage?tool=$tool"
        context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
      }
    }

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(R.string.edit_item_tools_title),
      style = MaterialTheme.typography.titleMedium,
    )

    ToolCard(
      title = stringResource(R.string.edit_item_make_m4b_title),
      description = stringResource(R.string.edit_item_make_m4b_desc),
    ) {
      OutlinedButton(onClick = { openManager("m4b") }) {
        Text(stringResource(R.string.edit_item_open_manager))
      }
    }

    ToolCard(
      title = stringResource(R.string.edit_item_embed_metadata_title),
      description = stringResource(R.string.edit_item_embed_metadata_desc),
    ) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(onClick = { openManager("embed") }) {
          Text(stringResource(R.string.edit_item_open_manager))
        }
        Button(
          onClick = { onEvent(EditItemEvent.EmbedMetadata) },
          enabled = !uiState.isToolWorking,
        ) {
          Text(stringResource(R.string.edit_item_quick_embed))
        }
      }
    }
  }
}

@Composable
private fun ToolCard(title: String, description: String, actions: @Composable () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth()) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(text = title, style = MaterialTheme.typography.titleSmall)
      Text(text = description, style = MaterialTheme.typography.bodySmall)
      actions()
    }
  }
}

@ShelfDroidPreview
@Composable
private fun ToolsTabPreview() {
  PreviewWrapper { ToolsTab(uiState = Defaults.EDIT_ITEM_UI_STATE, onEvent = {}) }
}
