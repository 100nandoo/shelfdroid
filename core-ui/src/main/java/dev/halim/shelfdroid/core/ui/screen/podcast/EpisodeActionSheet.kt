@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ListItemAction

@Composable
internal fun EpisodeActionSheet(
  title: String,
  podcastTitle: String,
  publishedAt: String,
  canEdit: Boolean,
  canDelete: Boolean,
  onDismiss: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  ModalBottomSheet(onDismissRequest = onDismiss) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = podcastTitle,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (publishedAt.isNotBlank()) {
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = publishedAt,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (canEdit || canDelete) {
      HorizontalDivider(modifier = Modifier.padding(16.dp))
    }

    if (canEdit) {
      ListItemAction(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        text = stringResource(R.string.edit),
        contentDescription = stringResource(R.string.edit),
        icon = R.drawable.edit,
        onClick = onEdit,
      )
    }
    if (canDelete) {
      ListItemAction(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        text = stringResource(R.string.delete),
        contentDescription = stringResource(R.string.delete),
        icon = R.drawable.delete,
        onClick = onDelete,
      )
    }
    Spacer(modifier = Modifier.height(32.dp))
  }
}
