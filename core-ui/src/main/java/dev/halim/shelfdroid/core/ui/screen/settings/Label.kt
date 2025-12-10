package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsClickLabel(
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyLarge,
  text: String,
  supportingText: String,
  onClick: () -> Unit = {},
) {
  Row(
    modifier = Modifier.clickable(onClick = onClick).padding(bottom = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column {
      Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(end = 12.dp),
      )
      Text(
        text = supportingText,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
      )
    }
    Spacer(modifier = Modifier.weight(1f))
    Icon(
      modifier = Modifier.size(20.dp),
      imageVector = Icons.Default.ChevronRight,
      contentDescription = stringResource(R.string.remove_genre),
      tint = MaterialTheme.colorScheme.primary,
    )
  }
}

@Composable
fun SettingsLabel(
  modifier: Modifier = Modifier,
  text: String,
  style: TextStyle = MaterialTheme.typography.titleLarge,
) {
  Text(
    text = text,
    style = style,
    color = MaterialTheme.colorScheme.tertiary,
    modifier = modifier.padding(bottom = 12.dp),
  )
}

@Composable
fun SettingsSublabel(
  modifier: Modifier = Modifier,
  text: String,
  style: TextStyle = MaterialTheme.typography.titleMedium,
) {
  Text(text = text, style = style, modifier = modifier)
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
      SettingsClickLabel(
        text = stringResource(R.string.playback),
        supportingText = stringResource(R.string.playback_settings_and_behaviour),
      )
      SettingsLabel(text = "Display")
      SettingsSwitchItem(Modifier, "Dark Theme", true, {}, "List View", true)
      SettingsSublabel(text = "Home Screen")
      SettingsSwitchItem(Modifier, "List View", true, {}, "List View", true)
    }
  }
}
