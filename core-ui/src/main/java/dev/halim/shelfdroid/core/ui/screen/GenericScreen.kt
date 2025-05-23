package dev.halim.shelfdroid.core.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun GenericMessageScreen(message: String) {
  val isMessageLong = message.length > 30
  Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
    Text(
      text = message,
      textAlign = TextAlign.Center,
      style =
        if (isMessageLong) MaterialTheme.typography.titleLarge
        else MaterialTheme.typography.headlineSmall,
    )
  }
}
