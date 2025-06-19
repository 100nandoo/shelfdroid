@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.components.player

import ItemCover
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.components.AutoSizeText
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SmallPlayerContent(
  id: String = Defaults.BOOK_ID,
  author: String = Defaults.BOOK_AUTHOR,
  title: String = Defaults.BOOK_TITLE,
  cover: String = Defaults.BOOK_COVER,
  progress: Float = Defaults.PROGRESS,
  onClicked: (String) -> Unit,
  onSwipeUp: () -> Unit,
  onSwipeDown: () -> Unit,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Column(
        Modifier.fillMaxWidth()
          .mySharedBound(Animations.Companion.Player.containerKey(id))
          .height(120.dp)
          .pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
              if (dragAmount < 0) {
                onSwipeUp()
              } else {
                onSwipeDown()
              }
            }
          }
          .clickable { onClicked(id) }
          .navigationBarsPadding()
      ) {
        LinearProgressIndicator(
          progress = { progress },
          Modifier.mySharedBound(Animations.Companion.Player.progressKey(id)).fillMaxWidth(),
          drawStopIndicator = {},
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
          ItemCover(
            Modifier.fillMaxHeight().padding(8.dp),
            cover = cover,
            animationKey = Animations.Companion.Player.coverKey(id),
            fontSize = 10.sp,
            shape = RoundedCornerShape(4.dp),
          )

          SmallPlayerInfo(Modifier.padding(8.dp).weight(1f, true), author, title)

          SmallPlayerControls(id)
        }
      }
    }
  }
}

@Composable
private fun SmallPlayerInfo(
  modifier: Modifier = Modifier,
  author: String = "",
  title: String = "",
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Column(modifier, verticalArrangement = Arrangement.Center) {
        AutoSizeText(
          modifier = Modifier.mySharedBound(Animations.Companion.Player.titleKey(title)),
          text = title,
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Start,
          maxLines = 2,
        )
        AutoSizeText(
          modifier =
            Modifier.mySharedBound(Animations.Companion.Player.authorKey(Defaults.BOOK_AUTHOR)),
          text = author,
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Start,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          overflow = TextOverflow.Ellipsis,
          maxLines = 2,
        )
      }
    }
  }
}

@Composable
private fun SmallPlayerControls(id: String = "") {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.seekBackKey(id)),
        icon = Icons.Default.Replay10,
        size = 32,
        contentDescription = "Seek Back 10s",
        onClick = {},
      )

      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.playKey(id)),
        icon = Icons.Default.PlayArrow,
        contentDescription = "Play Pause",
        size = 40,
        onClick = {},
      )

      MyIconButton(
        modifier = Modifier.mySharedBound(Animations.Companion.Player.seekForwardKey(id)),
        icon = Icons.Default.Forward10,
        contentDescription = "Seek Forward 10s",
        size = 32,
        onClick = {},
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun SmallPlayerContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    Column {
      Box(modifier = Modifier.weight(1f))
      SmallPlayerContent(onClicked = {}, onSwipeUp = {}, onSwipeDown = {})
    }
  }
}

@ShelfDroidPreview
@Composable
fun SmallPlayerContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) {
    Column {
      Box(modifier = Modifier.weight(1f))
      SmallPlayerContent(onClicked = {}, onSwipeUp = {}, onSwipeDown = {})
    }
  }
}
