package dev.halim.shelfdroid.widget.player

import android.graphics.Bitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.PlayerUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlayerWidgetImagesTest {
  @Test
  fun `size compositions share covers and replace them without stale artwork`() = runTest {
    val firstBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val secondBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val content = MutableStateFlow(PlayerUiState(id = "book", cover = "first").toPlayerWidgetData())
    val loads = mutableListOf<String>()
    val flow =
      playerWidgetContentFlow(backgroundScope, content) { cover ->
        loads += cover
        if (cover == "first") firstBitmap
        else {
          delay(100)
          secondBitmap
        }
      }
    val portrait = mutableListOf<PlayerWidgetData?>()
    val landscape = mutableListOf<PlayerWidgetData?>()
    backgroundScope.launch { flow.collect { portrait += it } }
    backgroundScope.launch { flow.collect { landscape += it } }
    runCurrent()

    assertEquals(listOf("first"), loads)
    assertSame(firstBitmap, portrait.last()?.coverBitmap)
    assertSame(firstBitmap, landscape.last()?.coverBitmap)

    content.value = content.value!!.copy(showPlayIcon = false)
    runCurrent()
    assertEquals(listOf("first"), loads)
    assertFalse(portrait.last()!!.showPlayIcon)
    assertSame(firstBitmap, portrait.last()?.coverBitmap)

    content.value = content.value!!.copy(cover = "second")
    runCurrent()
    assertEquals("second", portrait.last()?.cover)
    assertNull(portrait.last()?.coverBitmap)
    advanceTimeBy(100)
    runCurrent()
    assertSame(secondBitmap, portrait.last()?.coverBitmap)
    assertSame(secondBitmap, landscape.last()?.coverBitmap)
    assertEquals(listOf("first", "second"), loads)

    content.value = content.value!!.copy(cover = "")
    runCurrent()
    assertNull(portrait.last()?.coverBitmap)
    assertEquals(listOf("first", "second"), loads)
  }

  @Test
  fun `cover requests fit widget dimensions and the conservative byte budget`() {
    val sizes = listOf(DpSize(200.dp, 100.dp), DpSize(100.dp, 300.dp))
    assertEquals(200, widgetCoverTargetSize(sizes, density = 2f, memoryCapBytes = 1_000_000))
    assertEquals(80, widgetCoverTargetSize(sizes, density = 2f, memoryCapBytes = 80L * 80 * 4))
  }

  @Test
  fun `decoded images respect actual bytes and downsample extreme aspect ratios`() {
    val square = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    val bounded = fitPlayerWidgetBitmap(square, targetSize = 100, memoryCapBytes = 400)!!
    assertTrue(bounded.byteCount <= 400)
    assertFalse(square.isRecycled)

    val banner = Bitmap.createBitmap(1000, 20, Bitmap.Config.ARGB_8888)
    val thumbnail = fitPlayerWidgetBitmap(banner, targetSize = 40, memoryCapBytes = 6400)!!
    assertEquals(40, thumbnail.width)
    assertEquals(1, thumbnail.height)
    assertFalse(banner.isRecycled)
  }
}
