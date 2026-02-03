package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSION
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ListeningSessionItem(session: ListeningSessionUiState.Session) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
      Text(session.item.title, style = MaterialTheme.typography.titleSmall)
      Text(
        session.item.author,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        session.sessionTime.timeRange,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Text(session.sessionTime.duration, style = MaterialTheme.typography.titleMedium)
  }
}

@ShelfDroidPreview
@Composable
fun ListeningSessionItemPreview() {
  ListeningSessionItem(LISTENING_SESSION)
}
