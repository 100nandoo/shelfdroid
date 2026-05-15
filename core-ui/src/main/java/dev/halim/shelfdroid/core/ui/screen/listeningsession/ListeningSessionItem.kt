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
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
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
  showSheet: () -> Unit = {},
) {
  val onClick = {
    if (isSelectionMode) {
      onClickedAction()
    } else {
      showSheet()
    }
  }
  val onLongClick = {
    if (isSelectionMode.not()) {
      onLongClickedAction()
    }
  }
  val clickModifier =
    if (enableSelection) Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    else Modifier.clickable { showSheet() }
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
      TextTitleSmall(text = session.item.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
      TextLabelSmall(
        text = session.item.author,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      TextLabelSmall(
        text = session.sessionTime.timeRange,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
      TextTitleSmall(
        text = session.user.username ?: stringResource(R.string.unknown),
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

@ShelfDroidPreview
@Composable
fun ListeningSessionItemOverflowPreview() {
  val overflowSession =
    LISTENING_SESSION.copy(
      item =
        LISTENING_SESSION.item.copy(
          title =
            "An Extremely Long Listening Session Item Title That Should Be Trimmed In One Line",
          author = "Author With A Long Compound Name And Extra Context",
        ),
      sessionTime =
        LISTENING_SESSION.sessionTime.copy(
          timeRange = "Wednesday 14 May 2026, 11:00 PM to Thursday 15 May 2026, 1:30 AM"
        ),
      user =
        LISTENING_SESSION.user.copy(username = "listener-with-a-very-long-username@example.com"),
    )

  PreviewWrapper { LazyColumn { item { ListeningSessionItem(overflowSession) } } }
}
