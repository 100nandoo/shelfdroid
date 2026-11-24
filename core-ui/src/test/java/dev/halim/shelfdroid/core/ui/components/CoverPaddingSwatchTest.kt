package dev.halim.shelfdroid.core.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.Assert.assertEquals
import org.junit.Test

class CoverPaddingSwatchTest {
  @Test
  fun `dark padding boosts a muted chromatic swatch and rounds the blended channels`() {
    val result = CoverPaddingSwatch(hue = 0f, saturation = 0.1f).blendWith(Color.Black)
    assertEquals(0xff2e1b1b.toInt(), result.toArgb())
  }

  @Test
  fun `light padding keeps a vivid swatch saturated`() {
    val result = CoverPaddingSwatch(hue = 0f, saturation = 0.8f).blendWith(Color.White)
    assertEquals(0xfffdd8d8.toInt(), result.toArgb())
  }

  @Test
  fun `achromatic swatches stay neutral`() {
    val result = CoverPaddingSwatch(hue = 0f, saturation = 0f).blendWith(Color.Black)
    assertEquals(0xff242424.toInt(), result.toArgb())
  }
}
