package dev.halim.shelfdroid.core.ui.screen.settings

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsScreenContent() }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  val isDynamicTheme = true
  val uiState = SettingsUiState(isDynamicTheme = isDynamicTheme)
  PreviewWrapper(dynamicColor = isDynamicTheme) { SettingsScreenContent(uiState) }
}
