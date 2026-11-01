package dev.halim.shelfdroid.widget.player

import android.graphics.Bitmap
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.PlayerUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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
  fun `initial content and controls are available while the first cover loads`() = runTest {
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val data = PlayerUiState(id = "book", cover = "first").toPlayerWidgetData()!!
    val content = MutableStateFlow(data)
    val flow =
      playerWidgetContentFlow(backgroundScope, content) {
        delay(100)
        bitmap
      }
    val received = mutableListOf<PlayerWidgetData?>()
    backgroundScope.launch { received += flow.first() }
    runCurrent()
    assertEquals(listOf(data), received)

    backgroundScope.launch { flow.collect { received += it } }
    content.value = data.copy(showPlayIcon = false)
    runCurrent()
    assertFalse(received.last()!!.showPlayIcon)
    assertNull(received.last()!!.coverBitmap)

    advanceTimeBy(100)
    runCurrent()
    assertSame(bitmap, received.last()!!.coverBitmap)
    assertFalse(received.last()!!.showPlayIcon)
  }

  @Test
  fun `failed initial cover leaves content available and later covers can load`() = runTest {
    val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
    val data = PlayerUiState(id = "book", cover = "missing").toPlayerWidgetData()!!
    val content = MutableStateFlow(data)
    val flow =
      playerWidgetContentFlow(backgroundScope, content) { cover ->
        delay(100)
        if (cover == "missing") null else bitmap
      }
    val received = mutableListOf<PlayerWidgetData?>()
    backgroundScope.launch { flow.collect { received += it } }
    runCurrent()
    assertEquals(data, received.last())
    advanceTimeBy(100)
    runCurrent()
    assertEquals(data, received.last())

    content.value = data.copy(cover = "available")
    runCurrent()
    advanceTimeBy(100)
    runCurrent()
    assertSame(bitmap, received.last()!!.coverBitmap)
  }

  @Test
  fun `multiple size collectors share one upstream subscription and cancel obsolete covers`() =
    runTest {
      val bitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888)
      val content =
        MutableStateFlow(PlayerUiState(id = "book", cover = "first").toPlayerWidgetData())
      var subscriptions = 0
      val upstream = flow {
        subscriptions++
        content.collect { emit(it) }
      }
      val completed = mutableListOf<String>()
      val flow =
        playerWidgetContentFlow(backgroundScope, upstream) { cover ->
          delay(100)
          completed += cover
          bitmap
        }
      val portrait = mutableListOf<PlayerWidgetData?>()
      val landscape = mutableListOf<PlayerWidgetData?>()
      backgroundScope.launch { flow.collect { portrait += it } }
      backgroundScope.launch { flow.collect { landscape += it } }
      runCurrent()
      assertEquals(1, subscriptions)
      assertEquals("first", portrait.last()!!.cover)
      assertNull(portrait.last()!!.coverBitmap)

      advanceTimeBy(50)
      content.value = content.value!!.copy(cover = "second")
      runCurrent()
      advanceTimeBy(100)
      runCurrent()
      assertEquals(listOf("second"), completed)
      assertSame(bitmap, portrait.last()!!.coverBitmap)
      assertSame(bitmap, landscape.last()!!.coverBitmap)
      assertEquals(1, subscriptions)
    }

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
    val largePreview = listOf(DpSize(320.dp, 180.dp))
    assertEquals(360, widgetCoverTargetSize(largePreview, density = 2f, memoryCapBytes = 1_000_000))
    assertEquals(
      80,
      widgetCoverTargetSize(largePreview, density = 2f, memoryCapBytes = 80L * 80 * 4),
    )
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
