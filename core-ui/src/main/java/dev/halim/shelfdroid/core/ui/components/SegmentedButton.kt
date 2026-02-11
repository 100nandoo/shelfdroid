package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MySegmentedButton(
  modifier: Modifier = Modifier,
  options: List<String> = emptyList(),
  label: String? = null,
  initialValue: String = "",
  onClick: (String) -> Unit = {},
) {
  val initialIndex = options.indexOf(initialValue).coerceAtLeast(0)
  var selectedIndex by remember { mutableIntStateOf(initialIndex) }

  Column(modifier) {
    if (label != null) {
      Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
      options.forEachIndexed { index, option ->
        SegmentedButton(
          shape = SegmentedButtonDefaults.itemShape(index, options.size),
          selected = selectedIndex == index,
          onClick = {
            selectedIndex = index
            onClick(option)
          },
        ) {
          Text(option, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
      }
    }

    Spacer(Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
private fun MySegmentedButtonPreview() {
  PreviewWrapper {
    MySegmentedButton(
      label = "Filter: ",
      options = listOf("Option 1", "Option 2", "Option Very Long"),
      initialValue = "Option Very Long",
    )
  }
}
