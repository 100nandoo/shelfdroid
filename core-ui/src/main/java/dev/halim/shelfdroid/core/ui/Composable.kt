package dev.halim.shelfdroid.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import dev.halim.shelfdroid.core.ui.screen.MainActivity

@Composable
fun InitMediaControllerIfMainActivity() {
  val context = LocalContext.current
  LaunchedEffect(context) {
    if (context is MainActivity) {
      context.initMediaController()
    }
  }
}
