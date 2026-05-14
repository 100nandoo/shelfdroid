package dev.halim.shelfdroid.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun VisibilityUp(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible,
    exit = slideOutVertically(targetOffsetY = { fullHeight -> fullHeight }) + fadeOut(),
    enter = slideInVertically(initialOffsetY = { fullHeight -> fullHeight }) + fadeIn(),
  ) {
    content()
  }
}

@Composable
fun VisibilityDown(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
  ) {
    content()
  }
}

@Composable
fun VisibilityCircular(isLoading: Boolean, content: @Composable () -> Unit) {
  Box {
    VisibilityUp(isLoading) {
      CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
    }
    VisibilityDown(isLoading.not()) { content() }
  }
}

@Composable
fun VisibilityCenter(visible: Boolean, content: @Composable () -> Unit) {
  AnimatedVisibility(
    visible = visible,
    enter =
      scaleIn(
        initialScale = 0.8f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
      ) + fadeIn(),
    exit =
      scaleOut(
        targetScale = 0.8f,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
      ) + fadeOut(),
  ) {
    content()
  }
}

@ShelfDroidPreview
@Composable
private fun VisibilityStatesPreview() {
  PreviewWrapper(dynamicColor = false) {
    Column {
      VisibilityUp(visible = true) { Text("Slides in from the bottom") }
      Spacer(modifier = Modifier.size(12.dp))
      VisibilityDown(visible = true) { Text("Slides in from the top") }
      Spacer(modifier = Modifier.size(12.dp))
      VisibilityCircular(isLoading = true) { Text("Loaded content") }
      Spacer(modifier = Modifier.size(12.dp))
      VisibilityCenter(visible = true) { Button(onClick = {}) { Text("Centered action") } }
    }
  }
}
