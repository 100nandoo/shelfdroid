@file:OptIn(ExperimentalSharedTransitionApi::class)

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.halim.shelfdroid.core.data.home.BookUiState
import dev.halim.shelfdroid.core.data.home.ShelfdroidMediaItem
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.screen.home.HomeEvent

@Composable
fun Item(uiState: ShelfdroidMediaItem, onEvent: (HomeEvent) -> Unit = {}) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Card(
        modifier = Modifier.mySharedBound(Animations.containerKey(uiState.id)).padding(4.dp),
        onClick = { onEvent(HomeEvent.Navigate(uiState.id, uiState is BookUiState)) },
        shape = RoundedCornerShape(8.dp),
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth()) {
            ItemCover(Modifier.fillMaxWidth(), coverUrl = uiState.cover)
            if (uiState is BookUiState && uiState.progress > 0.0) {
              val progress = uiState.progress
              LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(12.dp),
                strokeCap = StrokeCap.Square,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                trackColor = Color.Transparent,
                drawStopIndicator = {},
              )
            }
          }
          Text(
            text = uiState.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
            modifier =
              Modifier.mySharedBound(Animations.titleKey(uiState.id, uiState.title))
                .padding(horizontal = 8.dp)
                .padding(top = 8.dp),
          )

          Text(
            text = uiState.author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier =
              Modifier.mySharedBound(Animations.authorKey(uiState.id, uiState.author))
                .padding(horizontal = 8.dp)
                .padding(top = 4.dp, bottom = 8.dp),
          )
        }
      }
    }
  }
}

@Composable
fun ItemCover(
  modifier: Modifier = Modifier,
  background: Color = MaterialTheme.colorScheme.secondaryContainer,
  textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  fontSize: TextUnit = 14.sp,
  coverUrl: String,
  shape: Shape = RoundedCornerShape(8.dp, 8.dp),
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      val animatedModifier =
        modifier.mySharedElement(Animations.coverKey(coverUrl), OverlayClip(shape))

      ItemCoverNoAnimation(
        modifier = animatedModifier,
        background = background,
        textColor = textColor,
        fontSize = fontSize,
        coverUrl = coverUrl,
        shape = shape,
      )
    }
  }
}

@Composable
fun ItemCoverNoAnimation(
  modifier: Modifier = Modifier,
  background: Color = MaterialTheme.colorScheme.secondaryContainer,
  textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  fontSize: TextUnit = 14.sp,
  coverUrl: String,
  shape: Shape = RoundedCornerShape(8.dp),
) {
  var imageLoadFailed by remember { mutableStateOf(false) }
  if (imageLoadFailed) {
    Box(
      modifier = modifier.aspectRatio(1f).background(background, shape = shape),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = "No cover", fontSize = fontSize, color = textColor, textAlign = TextAlign.Center)
    }
  } else {
    AsyncImage(
      modifier = modifier.clip(shape).background(background).aspectRatio(1f),
      model = coverUrl,
      contentDescription = "Library item cover image",
      onError = { imageLoadFailed = true },
    )
  }
}

@Composable
fun ItemDetail(id: String, url: String, title: String, authorName: String, subtitle: String = "") {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      Spacer(modifier = Modifier.height(16.dp))
      ItemCover(Modifier.fillMaxWidth(), coverUrl = url, shape = RoundedCornerShape(8.dp))
      Spacer(modifier = Modifier.height(16.dp))

      Text(
        modifier = Modifier.mySharedBound(Animations.titleKey(id, title)),
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        textAlign = TextAlign.Center,
      )

      if (subtitle.isNotEmpty()) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.titleMedium,
          textAlign = TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(modifier = Modifier.height(8.dp))

      Text(
        modifier = Modifier.mySharedBound(Animations.authorKey(id, authorName)),
        text = authorName,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.Gray,
        textAlign = TextAlign.Center,
      )
    }
  }
}
