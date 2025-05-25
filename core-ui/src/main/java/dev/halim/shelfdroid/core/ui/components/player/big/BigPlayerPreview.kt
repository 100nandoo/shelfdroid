package dev.halim.shelfdroid.core.ui.components.player.big

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun BigPlayerContentPreview() {
  PreviewWrapper(dynamicColor = false) { BigPlayerContent() }
}

@ShelfDroidPreview
@Composable
fun BigPlayerContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) { BigPlayerContent() }
}
