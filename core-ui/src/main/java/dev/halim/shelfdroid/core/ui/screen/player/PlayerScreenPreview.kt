package dev.halim.shelfdroid.core.ui.screen.player

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview


@ShelfDroidPreview
@Composable
fun PlayerScreenContentPreview() {
    PreviewWrapper(dynamicColor = false) {
        PlayerScreenContent()
    }
}

@ShelfDroidPreview
@Composable
fun PlayerScreenContentDynamicPreview() {
    PreviewWrapper(dynamicColor = true) {
        PlayerScreenContent()
    }
}