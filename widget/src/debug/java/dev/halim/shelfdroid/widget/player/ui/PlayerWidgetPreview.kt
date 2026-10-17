package dev.halim.shelfdroid.widget.player.ui

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import dev.halim.shelfdroid.core.ui.theme.darkScheme
import dev.halim.shelfdroid.core.ui.theme.lightScheme
import dev.halim.shelfdroid.widget.player.playerWidgetPreviewData

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 96)
@Composable
private fun PlayerWidgetLightPreview() {
  GlanceTheme(colors = ColorProviders(light = lightScheme, dark = lightScheme)) {
    PlayerWidgetContent(content = playerWidgetPreviewData)
  }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 96)
@Composable
private fun PlayerWidgetDarkPreview() {
  GlanceTheme(colors = ColorProviders(light = darkScheme, dark = darkScheme)) {
    PlayerWidgetContent(content = playerWidgetPreviewData)
  }
}
