package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSION
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LazyItemScope.ListeningSessionItem(
  session: ListeningSessionUiState.Session,
  enableSelection: Boolean = false,
  isSelected: Boolean = false,
  isSelectionMode: Boolean = false,
  onClickedAction: () -> Unit = {},
  onLongClickedAction: () -> Unit = {},
  showSheet: (ListeningSessionUiState.Session) -> Unit = {},
) {
  val onClick = {
    if (isSelectionMode) {
      onClickedAction()
    } else {
      showSheet(session)
    }
  }
  val onLongClick = {
    if (isSelectionMode.not()) {
      onLongClickedAction()
    }
  }
  val clickModifier =
    if (enableSelection) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    else Modifier.clickable { showSheet(session) }
  Row(
    modifier =
      Modifier.animateItem()
        .fillMaxWidth()
        .then(clickModifier)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AnimatedVisibility(isSelectionMode) {
      Checkbox(checked = isSelected, onCheckedChange = { onClickedAction() })
    }
    Column(modifier = Modifier.weight(3f).padding(end = 16.dp)) {
      Text(
        session.item.title,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        session.item.author,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        session.sessionTime.timeRange,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
      Text(
        text = session.user.username ?: stringResource(R.string.unknown),
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        session.sessionTime.duration,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun ListeningSessionItemPreview() {
  PreviewWrapper { LazyColumn { item { ListeningSessionItem(LISTENING_SESSION) } } }
}
