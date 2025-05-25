package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun IconButton(
  modifier: Modifier = Modifier,
  icon: ImageVector,
  contentDescription: String,
  size: Int = 48,
  onClick: () -> Unit,
) {
  Box(modifier = modifier.clip(CircleShape).clickable(onClick = onClick).size(size.dp)) {
    Icon(
      modifier = Modifier.size(size.dp).align(Alignment.Center).then(modifier),
      tint = MaterialTheme.colorScheme.onSecondaryContainer,
      imageVector = icon,
      contentDescription = contentDescription,
    )
  }
}
