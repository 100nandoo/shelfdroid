package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTextButton
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MetadataUtilsScreen(
  onTagsClicked: () -> Unit,
  onGenresClicked: () -> Unit = {},
  onCustomProvidersClicked: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxSize().padding(16.dp),
    verticalArrangement = Arrangement.Bottom,
  ) {
    Spacer(Modifier.height(16.dp))
    MyTextButton(
      onClick = onTagsClicked,
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.metadata_tags),
    )
    MyTextButton(
      onClick = onGenresClicked,
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.metadata_genres),
    )
    MyTextButton(
      onClick = onCustomProvidersClicked,
      modifier = Modifier.fillMaxWidth(),
      text = stringResource(R.string.metadata_custom_providers),
    )
  }
}

@ShelfDroidPreview
@Composable
private fun MetadataUtilsScreenPreview() {
  PreviewWrapper {
    MetadataUtilsScreen(
      onTagsClicked = {},
      onGenresClicked = {},
      onCustomProvidersClicked = {},
    )
  }
}
