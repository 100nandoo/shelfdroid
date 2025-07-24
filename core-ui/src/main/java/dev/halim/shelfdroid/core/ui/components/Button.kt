package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
  Box(
    modifier =
      Modifier.size(size.dp)
        .clip(CircleShape)
        .clickable(
          interactionSource = null,
          onClick = onClick,
          enabled = enabled,
          role = Role.Button,
          indication = ripple(false, size.dp),
        )
        .padding(4.dp)
        .then(modifier),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      modifier = Modifier.size(size.dp),
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
fun PlayButton(
  modifier: Modifier = Modifier,
  isPlaying: Boolean = false,
  onPlayClicked: () -> Unit,
) {
  val icon = if (isPlaying.not()) Icons.Default.PlayArrow else Icons.Default.Pause
  val contentDescription = if (isPlaying.not()) "Play" else "Pause"
  Button(
    onClick = { onPlayClicked() },
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
  ) {
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
  PreviewWrapper(content = { PlayButton(onPlayClicked = {}) })
}
