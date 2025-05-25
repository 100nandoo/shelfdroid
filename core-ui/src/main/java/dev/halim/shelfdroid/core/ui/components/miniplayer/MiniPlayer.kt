@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.components.miniplayer

import ItemCoverNoAnimation
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.halim.shelfdroid.core.extensions.dropShadow
import dev.halim.shelfdroid.core.ui.components.IconButton
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
  scaffoldState: BottomSheetScaffoldState,
  id: String,
  onClicked: (String) -> Unit,
  content: @Composable (PaddingValues) -> Unit,
) {
  val insets = WindowInsets.systemGestures.asPaddingValues()
  val gestureNavBottomInset = insets.calculateBottomPadding()

  BottomSheetScaffold(
    scaffoldState = scaffoldState,
    sheetPeekHeight = 120.dp,
    sheetDragHandle = {},
    sheetShape = RectangleShape,
    sheetContent = {
      MiniPlayerSheet(id = id, onClicked = onClicked, bottomInset = gestureNavBottomInset)
    },
  ) { paddingValues ->
    val padding by
      animateDpAsState(
        targetValue =
          if (scaffoldState.bottomSheetState.isVisible) paddingValues.calculateBottomPadding()
          else 0.dp
      )
    content(PaddingValues(bottom = padding))
  }
}

@Composable
private fun MiniPlayerSheet(
  id: String,
  onClicked: (String) -> Unit,
  bottomInset: androidx.compose.ui.unit.Dp,
) {
  Column(
    Modifier.fillMaxWidth()
      .height(120.dp)
      .clickable { onClicked(id) }
      .dropShadow(RectangleShape)
      .padding(bottom = bottomInset)
  ) {
    LinearProgressIndicator(progress = { 0.5f }, Modifier.fillMaxWidth(), drawStopIndicator = {})
    MiniPlayerContent()
  }
}

@Composable
private fun MiniPlayerContent() {
  Row(verticalAlignment = Alignment.CenterVertically) {
    ItemCoverNoAnimation(
      Modifier.fillMaxHeight().padding(8.dp),
      coverUrl = "",
      fontSize = 10.sp,
      shape = RoundedCornerShape(4.dp),
    )

    MiniPlayerInfo(Modifier.padding(8.dp).weight(1f, true))

    MiniPlayerControls()
  }
}

@Composable
private fun MiniPlayerInfo(modifier: Modifier = Modifier) {
  Column(modifier, verticalArrangement = Arrangement.Center) {
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
      textAlign = TextAlign.Center,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
    )
  }
}

@Composable
private fun MiniPlayerControls() {
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

@ShelfDroidPreview
@Composable
fun BottomSheetMiniPlayerPreview() {
  PreviewWrapper(dynamicColor = false) {
    MiniPlayer(rememberBottomSheetScaffoldState(), id = "", onClicked = {}, content = {})
  }
}
