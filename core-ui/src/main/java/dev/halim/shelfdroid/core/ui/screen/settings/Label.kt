package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsLabel(
  modifier: Modifier = Modifier,
  text: String,
  style: TextStyle = MaterialTheme.typography.titleLarge,
) {
  Text(text = text, style = style, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
}

@Composable
fun SettingsSublabel(
  modifier: Modifier = Modifier,
  text: String,
  style: TextStyle = MaterialTheme.typography.titleMedium,
) {
  Text(text = text, style = style, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
}

@Composable
fun SettingsBody(
  modifier: Modifier = Modifier,
  text: String,
  style: TextStyle = MaterialTheme.typography.bodySmall,
) {
  Text(text = text, style = style, modifier = modifier)
}

@ShelfDroidPreview
@Composable
fun SettingsLabelPreview() {
  PreviewWrapper {
    Column(Modifier.padding(16.dp)) {
      SettingsLabel(text = "Display")
      SettingsSwitchItem(Modifier, "Dark Theme", true, {}, "List View", true)
      SettingsSublabel(text = "Home Screen")
      SettingsSwitchItem(Modifier, "List View", true, {}, "List View", true)
    }
  }
}
