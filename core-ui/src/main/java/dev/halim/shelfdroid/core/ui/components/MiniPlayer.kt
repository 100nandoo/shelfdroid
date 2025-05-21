package dev.halim.shelfdroid.core.ui.components

import ItemCover
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.halim.shelfdroid.core.extensions.dropShadow
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MiniPlayer(id: String, onClicked: (String) -> Unit) {
  val insets = WindowInsets.systemGestures.asPaddingValues()
  val gestureNavBottomInset = insets.calculateBottomPadding()

  Column(
    Modifier.fillMaxWidth()
      .height(120.dp)
      .clickable { onClicked(id) }
      .dropShadow(RectangleShape)
      .background(MaterialTheme.colorScheme.secondaryContainer)
      .padding(bottom = gestureNavBottomInset)
  ) {
    LinearProgressIndicator(progress = { 0.5f }, Modifier.fillMaxWidth(), drawStopIndicator = {})
    Row(verticalAlignment = Alignment.CenterVertically) {
      ItemCover(
        Modifier.fillMaxHeight().padding(8.dp),
        background = MaterialTheme.colorScheme.primaryContainer,
        textColor = MaterialTheme.colorScheme.onPrimaryContainer,
        coverUrl = "",
        fontSize = 10.sp,
        shape = RoundedCornerShape(4.dp),
      )
      Column(Modifier.padding(8.dp).weight(1f, true), verticalArrangement = Arrangement.Center) {
        Text(
          text = Defaults.BOOK_TITLE,
          style = MaterialTheme.typography.titleLarge,
          textAlign = TextAlign.Start,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
        Text(
          text = Defaults.BOOK_AUTHOR,
          style = MaterialTheme.typography.bodyLarge,
          color = Color.Gray,
          textAlign = TextAlign.Center,
          overflow = TextOverflow.Ellipsis,
          maxLines = 1,
        )
      }

      IconButton(icon = Icons.Default.Replay10, contentDescription = "Seek Back 10s", onClick = {})

      IconButton(
        icon = Icons.Default.PlayArrow,
        contentDescription = "Play Pause",
        size = 72,
        onClick = {},
      )

      IconButton(
        Modifier.padding(end = 8.dp),
        icon = Icons.Default.Forward10,
        contentDescription = "Seek Forward 10s",
        onClick = {},
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun MiniPlayerPreview() {
  PreviewWrapper(dynamicColor = false) {
    Column(verticalArrangement = Arrangement.Bottom) {
      Box(Modifier.weight(1f))
      Box { MiniPlayer("", {}) }
    }
  }
}
