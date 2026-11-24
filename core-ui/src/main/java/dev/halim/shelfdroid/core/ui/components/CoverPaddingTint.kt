package dev.halim.shelfdroid.core.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.times
import androidx.compose.ui.unit.IntSize
import androidx.palette.graphics.Palette
import java.util.Collections
import java.util.WeakHashMap
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val paletteDispatcher = Dispatchers.Default.limitedParallelism(2)
private val coverSwatches = Collections.synchronizedMap(WeakHashMap<Bitmap, CoverPaddingSwatch>())
// Shared covers can be recomposed at a different decode size during navigation.
private val coverPaddingByUrl = LruCache<String, CoverPadding>(256)

private data class CoverPadding(val artworkSize: IntSize, val swatch: CoverPaddingSwatch)

class CoverPaddingSwatch
internal constructor(private val hue: Float, private val saturation: Float) {
  fun blendWith(background: Color): Color {
    val baseArgb = background.toArgb()
    val darkBackground = Color(baseArgb).luminance() < 0.5f
    val lightness = if (darkBackground) 0.42f else 0.66f
    // Match Harmonic's default colorfulness and keep achromatic swatches neutral.
    val minimumSaturation = if (saturation >= 0.05f) 0.25f else 0f
    val tint = Color.hsl(hue, (saturation * 1.1f).coerceIn(minimumSaturation, 1f), lightness)
    val tintArgb = tint.toArgb()
    val amount = if (darkBackground) 0.34f else 0.24f
    fun blendChannel(shift: Int): Int {
      val base = (baseArgb ushr shift) and 0xff
      val target = (tintArgb ushr shift) and 0xff
      return (base * (1f - amount) + target * amount).roundToInt() shl shift
    }
    return Color(blendChannel(24) or blendChannel(16) or blendChannel(8) or blendChannel(0))
  }
}

suspend fun loadCoverPaddingSwatch(bitmap: Bitmap): CoverPaddingSwatch? {
  if (bitmap.width == bitmap.height) return null
  return withContext(paletteDispatcher) {
    coverSwatches[bitmap] ?: extractCoverSwatch(bitmap)?.also { coverSwatches[bitmap] = it }
  }
}

@Composable
internal fun Modifier.coverPaddingTint(url: String, bitmap: Bitmap?, background: Color): Modifier {
  var padding by remember(url) { mutableStateOf(coverPaddingByUrl[url]) }
  LaunchedEffect(url, bitmap) {
    if (bitmap == null) return@LaunchedEffect
    if (bitmap.width == bitmap.height) {
      coverPaddingByUrl.remove(url)
      padding = null
      return@LaunchedEffect
    }
    val swatch = coverPaddingByUrl[url]?.swatch ?: loadCoverPaddingSwatch(bitmap)
    if (swatch != null) {
      val resolved =
        CoverPadding(IntSize(bitmap.width, bitmap.height), coverPaddingByUrl[url]?.swatch ?: swatch)
      coverPaddingByUrl.put(url, resolved)
      padding = resolved
    }
  }
  val artworkSize =
    bitmap?.let { IntSize(it.width, it.height) } ?: padding?.artworkSize ?: return this
  if (artworkSize.width == artworkSize.height) return this
  val swatch = padding?.swatch
  val paddingColor =
    key(url) {
      animateColorAsState(
        targetValue = swatch?.blendWith(background) ?: background.copy(alpha = 0f),
        label = "cover padding tint",
      )
    }
  if (swatch == null) return this

  return drawWithCache {
    val artwork = Size(artworkSize.width.toFloat(), artworkSize.height.toFloat())
    val fitted = artwork * ContentScale.Fit.computeScaleFactor(artwork, size)
    val fittedPixels = IntSize(fitted.width.roundToInt(), fitted.height.roundToInt())
    val offset =
      Alignment.Center.align(
        size = fittedPixels,
        space = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        layoutDirection = layoutDirection,
      )
    onDrawBehind {
      // Keep the theme background beneath the artwork, including any transparent pixels.
      clipRect(
        left = offset.x.toFloat(),
        top = offset.y.toFloat(),
        right = (offset.x + fittedPixels.width).toFloat(),
        bottom = (offset.y + fittedPixels.height).toFloat(),
        clipOp = ClipOp.Difference,
      ) {
        drawRect(paddingColor.value)
      }
    }
  }
}

private fun extractCoverSwatch(bitmap: Bitmap): CoverPaddingSwatch? {
  val sample = if (bitmap.hasAlpha()) sampleVisibleCoverPixels(bitmap) ?: return null else bitmap
  val palette = Palette.from(sample).clearFilters().generate()
  val minimumAccentPopulation = palette.swatches.sumOf { it.population } * 0.08f
  // Prefer a midtone accent with enough coverage to represent the artwork.
  val accent =
    listOfNotNull(palette.vibrantSwatch, palette.lightVibrantSwatch, palette.darkVibrantSwatch)
      .firstOrNull { it.population >= minimumAccentPopulation }
  val swatch =
    accent
      ?: palette.mutedSwatch
      ?: palette.lightMutedSwatch
      ?: palette.darkMutedSwatch
      ?: palette.dominantSwatch
      ?: return null
  val (hue, saturation) = swatch.hsl
  return CoverPaddingSwatch(hue, saturation)
}

private fun sampleVisibleCoverPixels(bitmap: Bitmap): Bitmap? {
  val scale = minOf(1f, 112f / maxOf(bitmap.width, bitmap.height))
  val width = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
  val height = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
  val sample = Bitmap.createScaledBitmap(bitmap, width, height, false)
  val pixels = IntArray(width * height)
  sample.getPixels(pixels, 0, width, 0, 0, width, height)

  // Palette counts every RGB value, so pass only pixels that contribute visible color.
  var visibleCount = 0
  for (pixel in pixels) {
    if (pixel ushr 24 != 0) pixels[visibleCount++] = pixel
  }
  return when (visibleCount) {
    0 -> null
    pixels.size -> sample
    else -> Bitmap.createBitmap(pixels, visibleCount, 1, Bitmap.Config.ARGB_8888)
  }
}
