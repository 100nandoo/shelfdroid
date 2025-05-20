package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { PodcastScreenContent() }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) { PodcastScreenContent() }
}
