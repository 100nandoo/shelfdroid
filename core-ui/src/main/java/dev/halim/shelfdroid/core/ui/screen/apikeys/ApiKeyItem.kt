package dev.halim.shelfdroid.core.ui.screen.apikeys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.apikeys.ApiKeyUi
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextLabelValue
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
import dev.halim.shelfdroid.core.ui.extensions.enable
import dev.halim.shelfdroid.core.ui.preview.Defaults.API_KEYS
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LazyItemScope.ApiKeyItem(
  apiKey: ApiKeyUi,
  onEditClicked: () -> Unit = {},
  onDeleteClicked: () -> Unit = {},
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  MyAlertDialog(
    title = stringResource(R.string.delete),
    text = stringResource(R.string.dialog_delete_text),
    showDialog = showDeleteDialog,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onDeleteClicked()
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
  )

  Row(
    modifier =
      Modifier.animateItem()
        .fillMaxWidth()
        .clickable(enabled = !apiKey.isExpired, onClick = onEditClicked)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextTitleSmall(
          modifier = Modifier.alignByBaseline(),
          text = "${apiKey.name} ∙ ",
          color = MaterialTheme.colorScheme.onSurface.enable(apiKey.isActive),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        TextLabelSmall(
          modifier = Modifier.alignByBaseline(),
          text = apiKey.owner,
          color = MaterialTheme.colorScheme.onSurfaceVariant.enable(apiKey.isActive),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      TextLabelValue(
        label = stringResource(R.string.api_key_last_used),
        value = apiKey.lastUsedAt ?: stringResource(R.string.never),
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurfaceVariant.enable(apiKey.isActive),
        labelWeight = 2f,
        valueWeight = 5f,
      )
      TextLabelValue(
        label = stringResource(R.string.api_key_expires_at),
        value =
          if (apiKey.isExpired) stringResource(R.string.api_key_expired)
          else apiKey.expiresAt ?: stringResource(R.string.never),
        color = MaterialTheme.colorScheme.onSurfaceVariant.enable(apiKey.isActive),
        modifier = Modifier.fillMaxWidth(),
        labelWeight = 2f,
        valueWeight = 5f,
      )
    }

    FilledTonalIconButton(onClick = { showDeleteDialog = true }) {
      Icon(
        painter = painterResource(id = R.drawable.delete),
        contentDescription = stringResource(R.string.delete),
      )
    }
  }
}

@ShelfDroidPreview
@Composable
private fun ApiKeyItemPreview() {
  PreviewWrapper { LazyColumn { items(API_KEYS, key = { it.id }) { ApiKeyItem(apiKey = it) } } }
}
