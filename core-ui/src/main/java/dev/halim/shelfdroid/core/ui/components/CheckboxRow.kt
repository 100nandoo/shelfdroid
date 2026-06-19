package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

@Composable
fun CheckboxRow(
  checked: Boolean,
  text: String,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
  Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    Text(text = text, style = textStyle)
  }
}
