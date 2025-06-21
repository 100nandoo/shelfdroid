package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun MyIconButton(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  contentDescription: String,
  size: Int = 48,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  IconButton(
    modifier =
      Modifier.clip(CircleShape)
        .clickable(onClick = onClick)
        .size(size.dp)
        .padding(4.dp)
        .then(modifier),
    enabled = enabled,
    onClick = onClick,
  ) {
    Icon(
      modifier = Modifier.size(size.dp).then(modifier),
      tint = MaterialTheme.colorScheme.onSecondaryContainer,
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
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
