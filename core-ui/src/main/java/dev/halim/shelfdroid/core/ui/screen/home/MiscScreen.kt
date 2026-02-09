package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MiscScreen(onListeningSessionClicked: () -> Unit = {}, onSettingsClicked: () -> Unit = {}) {
  MiscScreenContent(onListeningSessionClicked, onSettingsClicked)
}

@Composable
private fun MiscScreenContent(
  onListeningSessionClicked: () -> Unit = {},
  onSettingsClicked: () -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    TextButton(
      onClick = onListeningSessionClicked,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Text(text = stringResource(R.string.listening_sessions))
    }

    TextButton(
      onClick = onSettingsClicked,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Text(text = stringResource(R.string.settings))
    }
    Spacer(Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun MiscScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { MiscScreenContent() }
}
