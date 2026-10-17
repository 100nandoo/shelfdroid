package dev.halim.shelfdroid.widget.player

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme

private val previewPlayerWidgetData =
  PlayerWidgetData(
    id = "preview",
    episodeId = "preview",
    title = "The Fellowship of the Ring",
    author = "J. R. R. Tolkien",
    cover = "",
    playPauseEnabled = true,
    showPlayIcon = false,
    seekBackEnabled = true,
    seekForwardEnabled = true,
  )

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 96)
@Composable
private fun PlayerWidgetLightPreview() {
  GlanceTheme(colors = ColorProviders(light = lightScheme, dark = lightScheme)) {
    PlayerWidgetContent(content = previewPlayerWidgetData)
  }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 96)
@Composable
private fun PlayerWidgetDarkPreview() {
  GlanceTheme(colors = ColorProviders(light = darkScheme, dark = darkScheme)) {
    PlayerWidgetContent(content = previewPlayerWidgetData)
  }
}
