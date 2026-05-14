package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun HorizontalDividerNoLast(size: Int, i: Int) {
  if (i != size - 1) {
    HorizontalDivider()
  }
}

@Composable
fun HorizontalDividerNoFirst(i: Int) {
  if (i != 0) {
    HorizontalDivider()
  }
}

@ShelfDroidPreview
@Composable
private fun HorizontalDividerPreview() {
  PreviewWrapper {
    Column(modifier = Modifier.padding(16.dp)) {
      Text("First item")
      HorizontalDividerNoLast(size = 3, i = 0)
      Text("Middle item")
      HorizontalDividerNoFirst(i = 1)
      HorizontalDividerNoLast(size = 3, i = 1)
      Text("Last item")
      HorizontalDividerNoFirst(i = 2)
      HorizontalDividerNoLast(size = 3, i = 2)
    }
  }
}
