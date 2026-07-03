package dev.halim.shelfdroid.core.ui.screen.editepisode.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyOutlinedTextField

@Composable
internal fun EditEpisodeDetailsTab(
  podcastTitle: String,
  title: String,
  canSave: Boolean,
  onTitleChange: (String) -> Unit,
  onSave: () -> Unit,
) {
  val focusManager = LocalFocusManager.current

  Column(
    modifier = Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    if (podcastTitle.isNotBlank()) {
      Text(
        text = podcastTitle,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 16.dp),
      )
    }

    MyOutlinedTextField(
      modifier = Modifier.padding(top = if (podcastTitle.isBlank()) 16.dp else 0.dp),
      value = title,
      onValueChange = onTitleChange,
      label = stringResource(R.string.title),
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
      onDone = { focusManager.clearFocus() },
    )

    Row(
      modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Button(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save)) }
    }
  }
}
