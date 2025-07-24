package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun SettingsLabel(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.labelLarge,
) {
  Text(text = text, style = style, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
}

@Composable
fun SettingsBody(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
  Text(text = text, style = style, modifier = modifier)
}
