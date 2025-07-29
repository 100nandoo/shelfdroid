package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MyFilledTonalIconButton(
  modifier: Modifier = Modifier,
  size: Int = 48,
  enabled: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  contentDescription: String,
) {
  require(size >= 48) { "Size must be at least 48" }
  val iconPadding = size / 6

  FilledTonalIconButton(
    modifier = Modifier.size(size.dp).then(modifier),
    enabled = enabled,
    onClick = { onClick() },
  ) {
    Icon(
      modifier = Modifier.size(size.dp).padding(iconPadding.dp),
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
}

@ShelfDroidPreview
@Composable
fun MyFilledTonalIconButtonPreview() {
  AnimatedPreviewWrapper(
    dynamicColor = false,
    content = {
      Row {
        MyFilledTonalIconButton(
          enabled = true,
          size = 72,
          onClick = {},
          icon = Icons.Default.PlayArrow,
          contentDescription = "",
        )
        MyFilledTonalIconButton(
          enabled = true,
          onClick = {},
          icon = Icons.Default.Pause,
          contentDescription = "",
        )
      }
    },
  )
}
