package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall

@Composable
fun MetadataUtilsScreen(
  onTagsClicked: () -> Unit,
  onGenresClicked: () -> Unit = {},
  onCustomProvidersClicked: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
    TextHeadlineSmall(text = stringResource(R.string.library_item_metadata_utilities))
    Spacer(Modifier.height(8.dp))
    Text(stringResource(R.string.metadata_utilities_description))
    Spacer(Modifier.height(16.dp))
    Button(onClick = onTagsClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_tags))
    }
    TextButton(onClick = onGenresClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_genres))
    }
    TextButton(onClick = onCustomProvidersClicked, modifier = Modifier.fillMaxWidth()) {
      Text(stringResource(R.string.metadata_custom_providers))
    }
  }
}
