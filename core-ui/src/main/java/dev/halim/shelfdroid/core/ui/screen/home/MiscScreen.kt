package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MiscScreen(onListeningSessionClicked: () -> Unit = {}) {
  MiscScreenContent(onListeningSessionClicked)
}

@Composable
private fun MiscScreenContent(onListeningSessionClicked: () -> Unit = {}) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    Box(
      modifier =
        Modifier.fillMaxWidth()
          .clickable(onClick = onListeningSessionClicked)
          .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
      Text(text = "Listening Sessions", style = MaterialTheme.typography.titleMedium)
    }
  }
}

@ShelfDroidPreview
@Composable
fun MiscScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { MiscScreenContent() }
}
