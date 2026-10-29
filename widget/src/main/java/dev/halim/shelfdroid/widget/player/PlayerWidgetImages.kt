package dev.halim.shelfdroid.widget.player

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.view.WindowManager
import androidx.compose.ui.unit.DpSize
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.size.Precision
import coil3.toBitmap
import kotlin.math.max
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext

// Share one loader and Bitmap across every SizeMode.Exact composition in this session.
@OptIn(ExperimentalCoroutinesApi::class)
internal fun playerWidgetContentFlow(
  scope: CoroutineScope,
  content: Flow<PlayerWidgetData?>,
  loadCover: suspend (String) -> Bitmap?,
) =
  combine(
      content,
      content
        .map { it?.cover.orEmpty() }
        .distinctUntilChanged()
        .mapLatest { cover ->
          cover to if (cover.isBlank()) null else loadCover(cover)
        },
    ) { data, (cover, bitmap) ->
      data?.copy(coverBitmap = bitmap.takeIf { data.cover == cover })
    }
    .shareIn(scope, SharingStarted.Eagerly, replay = 1)

internal fun widgetMemoryCapBytes(context: Context): Long {
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val bounds = context.getSystemService(WindowManager::class.java)?.currentWindowMetrics?.bounds
    if (bounds != null && bounds.width() > 0 && bounds.height() > 0) {
      return 6L * bounds.width() * bounds.height()
    }
  }
  val metrics = context.resources.displayMetrics
  return (6L * metrics.widthPixels * metrics.heightPixels).coerceAtLeast(4L)
}

internal fun widgetCoverTargetSize(
  sizes: List<DpSize>,
  density: Float,
  memoryCapBytes: Long,
): Int {
  val widgetSideDp = sizes.maxOf { minOf(it.width.value, it.height.value) }
  // Allow for translucent images being decoded as ARGB_8888 instead of RGB_565.
  val budgetSidePx = sqrt(memoryCapBytes.coerceAtLeast(4L) / 4.0).toInt().coerceAtLeast(1)
  return (widgetSideDp * density).toInt().coerceIn(1, budgetSidePx)
}

internal suspend fun loadPlayerWidgetCover(
  context: Context,
  cover: String,
  targetSize: Int,
  memoryCapBytes: Long,
): Bitmap? =
  withContext(Dispatchers.IO) {
    val request =
      ImageRequest.Builder(context)
        .data(cover)
        .size(targetSize)
        .precision(Precision.EXACT)
        .allowHardware(false)
        .allowRgb565(true)
        .build()
    val bitmap =
      SingletonImageLoader.get(context).execute(request).image?.toBitmap()
        ?: return@withContext null
    fitPlayerWidgetBitmap(bitmap, targetSize, memoryCapBytes)
  }

internal fun fitPlayerWidgetBitmap(bitmap: Bitmap, targetSize: Int, memoryCapBytes: Long): Bitmap? {
  // Check actual bytes too: decoders and custom image loaders can return a different config.
  val scale =
    minOf(
      1.0,
      targetSize.toDouble() / max(bitmap.width, bitmap.height),
      sqrt(memoryCapBytes.coerceAtLeast(1L).toDouble() / bitmap.byteCount.coerceAtLeast(1)),
    )
  var result =
    Bitmap.createScaledBitmap(
      bitmap,
      (bitmap.width * scale).toInt().coerceAtLeast(1),
      (bitmap.height * scale).toInt().coerceAtLeast(1),
      true,
    )
  while (result.byteCount.toLong() > memoryCapBytes) {
    if (result.width == 1 && result.height == 1) return null
    result =
      Bitmap.createScaledBitmap(
        result,
        (result.width / 2).coerceAtLeast(1),
        (result.height / 2).coerceAtLeast(1),
        true,
      )
  }
  return result
}
