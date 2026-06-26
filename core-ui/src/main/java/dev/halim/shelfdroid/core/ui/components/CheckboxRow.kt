package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun CheckboxRow(
  checked: Boolean,
  text: String,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
  wholeRowClickable: Boolean = false,
) {
  val rowModifier =
    if (wholeRowClickable) {
      modifier.toggleable(
        value = checked,
        enabled = enabled,
        role = Role.Checkbox,
        onValueChange = onCheckedChange,
      )
    } else {
      modifier
    }

  Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
    val checkboxModifier =
      if (wholeRowClickable) {
        Modifier.padding(12.dp)
      } else {
        Modifier
      }
    Checkbox(
      modifier = checkboxModifier,
      checked = checked,
      onCheckedChange = if (wholeRowClickable) null else onCheckedChange,
      enabled = enabled,
    )
    Text(text = text, style = textStyle)
  }
}

@Composable
private fun SampleCheckboxRows() {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    CheckboxRow(
      modifier = Modifier.fillMaxWidth(),
      checked = true,
      text = "Checked",
      onCheckedChange = {},
    )
    CheckboxRow(
      modifier = Modifier.fillMaxWidth(),
      checked = false,
      text = "Whole row clickable",
      onCheckedChange = {},
      wholeRowClickable = true,
    )
    CheckboxRow(
      modifier = Modifier.fillMaxWidth(),
      checked = false,
      text = "Disabled",
      onCheckedChange = {},
      enabled = false,
      wholeRowClickable = true,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun CheckboxRowPreview() {
  AnimatedPreviewWrapper { SampleCheckboxRows() }
}
