package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.halim.shelfdroid.core.ui.components.ExposedDropdownMenu

@Composable
fun SettingsDropDown(
  text: String,
  options: List<String>,
  initialValue: String,
  onClick: (String) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      modifier = Modifier.weight(1f),
      text = text,
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurface,
    )
    ExposedDropdownMenu(
      modifier = Modifier.weight(1f),
      options = options,
      initialValue = initialValue,
      onClick = onClick,
    )
  }
}
