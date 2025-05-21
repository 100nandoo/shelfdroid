@file:OptIn(ExperimentalSharedTransitionApi::class)

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
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
import dev.halim.shelfdroid.core.ui.screen.home.HomeEvent

@Composable
fun Item(
  modifier: Modifier = Modifier,
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  uiState: ShelfdroidMediaItem,
  onEvent: (HomeEvent) -> Unit = {},
) {
  with(sharedTransitionScope) {
    Card(
      modifier = modifier.padding(4.dp),
      onClick = { onEvent(HomeEvent.Navigate(uiState.id, uiState is BookUiState)) },
      shape = RoundedCornerShape(8.dp),
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomCenter, modifier = Modifier.fillMaxWidth()) {
          ItemCover(
            Modifier.fillMaxWidth(),
            sharedTransitionScope = sharedTransitionScope,
            animatedContentScope = animatedContentScope,
            coverUrl = uiState.cover,
          )
          if (uiState is BookUiState && uiState.progress > 0.0) {
            val float = uiState.progress
            LinearProgressIndicator(
              progress = { float },
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
            Modifier.sharedBounds(
                rememberSharedContentState(key = "${uiState.id}${uiState.title}"),
                animatedVisibilityScope = animatedContentScope,
              )
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
            Modifier.sharedBounds(
                rememberSharedContentState(key = "${uiState.id}${uiState.author}"),
                animatedVisibilityScope = animatedContentScope,
              )
              .padding(horizontal = 8.dp)
              .padding(top = 4.dp, bottom = 8.dp),
        )
      }
    }
  }
}

@Composable
fun ItemCover(
  modifier: Modifier = Modifier,
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  background: Color = MaterialTheme.colorScheme.secondaryContainer,
  textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  fontSize: TextUnit = 14.sp,
  coverUrl: String,
  shape: Shape = RoundedCornerShape(8.dp, 8.dp),
) {
  var imageLoadFailed by remember { mutableStateOf(false) }
  with(sharedTransitionScope) {
    if (imageLoadFailed) {
      Box(
        modifier = modifier.aspectRatio(1f).background(background, shape = shape),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = "No cover",
          fontSize = fontSize,
          color = textColor,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      AsyncImage(
        modifier =
          modifier
            .sharedElement(
              sharedTransitionScope.rememberSharedContentState(key = "cover_${coverUrl}"),
              animatedVisibilityScope = animatedContentScope,
              clipInOverlayDuringTransition = OverlayClip(shape),
            )
            .clip(shape)
            .background(background)
            .aspectRatio(1f),
        model = coverUrl,
        contentDescription = "Library item cover image",
        onError = { imageLoadFailed = true },
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
fun ItemDetail(
  sharedTransitionScope: SharedTransitionScope,
  animatedContentScope: AnimatedContentScope,
  id: String,
  url: String,
  title: String,
  authorName: String,
  subtitle: String = "",
) {
  with(sharedTransitionScope) {
    Spacer(modifier = Modifier.height(16.dp))
    ItemCover(
      Modifier.fillMaxWidth(),
      sharedTransitionScope = sharedTransitionScope,
      animatedContentScope = animatedContentScope,
      coverUrl = url,
      shape = RoundedCornerShape(8.dp),
    )
    Spacer(modifier = Modifier.height(16.dp))

    Text(
      modifier =
        Modifier.sharedBounds(
          rememberSharedContentState(key = "$id$title"),
          animatedVisibilityScope = animatedContentScope,
        ),
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
      modifier =
        Modifier.sharedBounds(
          rememberSharedContentState(key = "$id$authorName"),
          animatedVisibilityScope = animatedContentScope,
        ),
      text = authorName,
      style = MaterialTheme.typography.bodyMedium,
      color = Color.Gray,
      textAlign = TextAlign.Center,
    )
  }
}
