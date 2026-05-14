package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable

@Composable
private fun ScopedPreviewSurface(
  animated: Boolean,
  dynamicColor: Boolean,
  content: @Composable () -> Unit,
) {
  if (animated) {
    AnimatedPreviewWrapper(dynamicColor = dynamicColor, content = content)
  } else {
    PreviewWrapper(dynamicColor = dynamicColor, content = content)
  }
}

@Composable
fun BoxScopePreviewWrapper(
  animated: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable BoxScope.() -> Unit,
) {
  ScopedPreviewSurface(animated = animated, dynamicColor = dynamicColor) { Box(content = content) }
}

@Composable
fun RowScopePreviewWrapper(
  animated: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable RowScope.() -> Unit,
) {
  ScopedPreviewSurface(animated = animated, dynamicColor = dynamicColor) { Row(content = content) }
}

@Composable
fun LazyItemPreviewWrapper(
  animated: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable LazyItemScope.() -> Unit,
) {
  ScopedPreviewSurface(animated = animated, dynamicColor = dynamicColor) {
    LazyColumn { item(content = content) }
  }
}

@Composable
fun LazyGridItemPreviewWrapper(
  columns: GridCells = GridCells.Fixed(1),
  animated: Boolean = false,
  dynamicColor: Boolean = false,
  reverseLayout: Boolean = false,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable LazyGridItemScope.() -> Unit,
) {
  ScopedPreviewSurface(animated = animated, dynamicColor = dynamicColor) {
    LazyVerticalGrid(
      columns = columns,
      reverseLayout = reverseLayout,
      verticalArrangement = verticalArrangement,
    ) {
      item(content = content)
    }
  }
}
