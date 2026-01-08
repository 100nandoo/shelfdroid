package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.mySharedElement

@Composable
fun Cover(
  modifier: Modifier = Modifier,
  background: Color = MaterialTheme.colorScheme.secondaryContainer,
  textColor: Color = MaterialTheme.colorScheme.onSecondaryContainer,
  fontSize: TextUnit = 14.sp,
  cover: String,
  animationKey: String = Animations.coverKey(cover),
  shape: Shape = RoundedCornerShape(8.dp, 8.dp),
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current

  with(sharedTransitionScope) {
    val animatedModifier = modifier.mySharedElement(animationKey, OverlayClip(shape))

    CoverNoAnimation(
      modifier = animatedModifier,
      background = background,
      textColor = textColor,
      fontSize = fontSize,
      coverUrl = cover,
      shape = shape,
    )
  }
}

@Composable
fun CoverNoAnimation(
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
      Text(
        text = stringResource(R.string.no_cover),
        fontSize = fontSize,
        color = textColor,
        textAlign = TextAlign.Center,
      )
    }
  } else {
    AsyncImage(
      modifier = modifier.clip(shape).background(background).aspectRatio(1f),
      model = ImageRequest.Builder(LocalContext.current).data(coverUrl).build(),
      contentDescription = stringResource(R.string.library_item_cover_image),
      onError = { imageLoadFailed = true },
    )
  }
}

@Composable
fun CoverWithTitle(
  cover: String,
  coverAnimationKey: String,
  title: String,
  titleAnimationKey: String,
  subtitle: String,
  subtitleAnimationKey: String,
) {
  Row(Modifier.height(IntrinsicSize.Max)) {
    Cover(
      Modifier.weight(1f).fillMaxHeight(),
      cover = cover,
      animationKey = coverAnimationKey,
      fontSize = 10.sp,
      shape = RoundedCornerShape(4.dp),
    )

    Column(
      Modifier.weight(4f).padding(8.dp).fillMaxHeight(),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        modifier = Modifier.mySharedElement(titleAnimationKey),
        text = title,
        style = MaterialTheme.typography.titleLarge,
      )
      Text(
        modifier = Modifier.mySharedElement(subtitleAnimationKey),
        text = subtitle,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Start,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
