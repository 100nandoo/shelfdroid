package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MyIconButton(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  contentDescription: String,
  size: Int = 48,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  require(size >= 48) { "Size must be at least 48" }
  val iconPadding = (size / 6).dp
  IconButton(
    modifier = Modifier.size(size.dp).then(modifier),
    onClick = onClick,
    enabled = enabled,
  ) {
    Icon(
      modifier = Modifier.size(size.dp).padding(iconPadding),
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
}

@Composable
private fun SampleMyIconButton(size: Int = 48) {
  MyIconButton(
    size = size,
    icon = Icons.Filled.PlayArrow,
    contentDescription = stringResource(R.string.play),
    onClick = {},
  )
}

@ShelfDroidPreview
@Composable
fun MyIconButtonPreview() {
  PreviewWrapper(content = { SampleMyIconButton(48) })
}

@ShelfDroidPreview
@Composable
fun MyIconButtonLargePreview() {
  PreviewWrapper(content = { SampleMyIconButton(72) })
}

@Composable
fun RowScope.PlayButton(
  modifier: Modifier = Modifier,
  isPlaying: Boolean = false,
  onPlayClicked: () -> Unit,
) {
  val icon = if (isPlaying.not()) Icons.Default.PlayArrow else Icons.Default.Pause
  val contentDescription = if (isPlaying.not()) "Play" else "Pause"
  Button(onClick = { onPlayClicked() }, modifier = modifier.weight(1f)) {
    Icon(
      imageVector = icon,
      contentDescription = contentDescription,
      modifier = Modifier.padding(end = 8.dp),
    )
    Text(contentDescription)
  }
}

@ShelfDroidPreview
@Composable
fun PlayButtonPreview() {
  PreviewWrapper(content = { Row { PlayButton(onPlayClicked = {}) } })
}
