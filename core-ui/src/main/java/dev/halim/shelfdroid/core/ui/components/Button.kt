package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
  Box(modifier = Modifier.size((size + 4).dp)) {
    IconButton(
      modifier = Modifier.align(Alignment.Center).clip(CircleShape).then(modifier),
      enabled = enabled,
      onClick = onClick,
    ) {
      Icon(
        modifier = Modifier.size(size.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        imageVector = icon,
        contentDescription = contentDescription,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun MyIconButtonPreview() {
  MyIconButton(icon = Icons.Filled.PlayArrow, contentDescription = "Play Icon Button", onClick = {})
}

@Composable
fun PlayButton(modifier: Modifier = Modifier, onPlayClicked: () -> Unit) {
  Button(
    onClick = { onPlayClicked() },
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
  ) {
    Icon(
      imageVector = Icons.Filled.PlayArrow,
      contentDescription = "Play",
      modifier = Modifier.padding(end = 8.dp),
    )
    Text("Play")
  }
}

@Preview
@Composable
fun PlayButtonPreview() {
  PlayButton(onPlayClicked = {})
}
