package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsSwitchItem(
  modifier: Modifier = Modifier,
  title: String,
  checked: Boolean,
  contentDescription: String,
  enabled: Boolean = true,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = modifier.fillMaxWidth().alpha(enabled.enableAlpha()),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Switch(
      enabled = enabled,
      modifier = Modifier.semantics { this.contentDescription = contentDescription },
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}

@Composable
private fun SampleSettingsSwitchItems() {
  Column {
    SettingsSwitchItem(
      title = "Dark Mode",
      checked = true,
      onCheckedChange = {},
      contentDescription = "Toggle Dark Mode",
      enabled = true,
    )
    SettingsSwitchItem(
      title = "Dynamic Theme",
      checked = false,
      onCheckedChange = {},
      contentDescription = "Toggle Dynamic Theme",
      enabled = true,
    )
    SettingsSwitchItem(
      title = "List View",
      checked = false,
      onCheckedChange = {},
      contentDescription = "Toggle List View",
      enabled = false,
    )
  }
}

@ShelfDroidPreview
@Composable
fun SettingsSwitchItemPreview() {
  PreviewWrapper(content = { SampleSettingsSwitchItems() })
}
