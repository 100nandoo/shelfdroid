package dev.halim.shelfdroid.core.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun GenericMessageScreen(message: String) {
  val isMessageLong = message.length > 30
  Box(
    modifier = Modifier.fillMaxSize().padding(16.dp),
    contentAlignment = Alignment.Center,
  ) {
    Text(
      text = message,
      textAlign = TextAlign.Center,
      style =
        if (isMessageLong) MaterialTheme.typography.titleLarge
        else MaterialTheme.typography.headlineSmall,
    )
  }
}

@Composable
fun GenericMessageActionScreen(
  message: String,
  actionLabel: String,
  onAction: () -> Unit,
) {
  Box(
    modifier = Modifier.fillMaxSize().padding(24.dp),
    contentAlignment = Alignment.BottomCenter,
  ) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        text = message,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge,
      )
      Spacer(modifier = Modifier.height(16.dp))
      Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) { Text(actionLabel) }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun GenericMessageScreenShortPreview() {
  PreviewWrapper(dynamicColor = false) { GenericMessageScreen(message = "No backups found") }
}

@ShelfDroidPreview
@Composable
private fun GenericMessageScreenLongPreview() {
  PreviewWrapper(dynamicColor = false) {
    GenericMessageScreen(
      message = "No API keys are available for this server yet. Create one to continue."
    )
  }
}

@ShelfDroidPreview
@Composable
private fun GenericMessageActionScreenPreview() {
  PreviewWrapper(dynamicColor = false) {
    GenericMessageActionScreen(
      message = "The server could not be reached.",
      actionLabel = "Retry",
      onAction = {},
    )
  }
}
