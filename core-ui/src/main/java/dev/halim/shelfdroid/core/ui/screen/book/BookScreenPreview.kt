package dev.halim.shelfdroid.core.ui.screen.book

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun BookScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { BookScreenContent(onPlayClicked = {}) }
}

@ShelfDroidPreview
@Composable
fun BookScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) { BookScreenContent(onPlayClicked = {}) }
}
