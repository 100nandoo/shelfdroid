package dev.halim.shelfdroid.core.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReorderableLazyListStateTest {

  @Test
  fun findReorderTarget_supportsDifferentItemHeightsInBothDirections() {
    val bounds =
      listOf(
        ReorderableItemBounds("a", offset = 0, size = 40),
        ReorderableItemBounds("b", offset = 40, size = 120),
        ReorderableItemBounds("c", offset = 160, size = 60),
      )

    assertEquals(
      "c",
      findReorderTarget(
        orderedKeys = listOf("a", "b", "c"),
        currentKey = "b",
        draggedCenter = 200f,
        visibleItems = bounds,
      ),
    )
    assertEquals(
      "a",
      findReorderTarget(
        orderedKeys = listOf("a", "b", "c"),
        currentKey = "c",
        draggedCenter = 10f,
        visibleItems = bounds,
      ),
    )
  }

  @Test
  fun cancelledDrag_restoresTheSourceOrder() {
    val model =
      ReorderableListModel(listOf("a", "b", "c"))
        .startDrag("a", pointerY = 20f)
        .moveTo("c", translationAdjustment = 0f)

    assertEquals(listOf("a", "b", "c"), model.cancelDrag().displayedKeys)
  }

  @Test
  fun unchangedDrop_doesNotProduceAMove() {
    val result = ReorderableListModel(listOf("a", "b")).startDrag("a", pointerY = 20f).finishDrag()

    assertNull(result.move)
    assertEquals(listOf("a", "b"), result.model.displayedKeys)
  }

  @Test
  fun completedDrop_isRetainedUntilTheCallerCatchesUp() {
    val result =
      ReorderableListModel(listOf("a", "b", "c"))
        .startDrag("a", pointerY = 20f)
        .moveTo("c", translationAdjustment = 0f)
        .finishDrag()

    assertEquals(ReorderMove("a", 2), result.move)
    assertEquals(
      listOf("b", "c", "a"),
      result.model
        .sync(
          incomingKeys = listOf("a", "b", "c"),
          callerChanged = true,
          enabled = true,
        )
        .displayedKeys,
    )
  }

  @Test
  fun externalUpdateDuringDrag_cancelsAndAdoptsTheCallerOrder() {
    val model =
      ReorderableListModel(listOf("a", "b", "c"))
        .startDrag("a", pointerY = 20f)
        .moveTo("b", translationAdjustment = 0f)

    val updated =
      model.sync(
        incomingKeys = listOf("c", "b", "a"),
        callerChanged = true,
        enabled = true,
      )

    assertEquals(listOf("c", "b", "a"), updated.displayedKeys)
    assertNull(updated.drag)
  }

  @Test
  fun disablingReorderDuringDrag_cancelsTheGesture() {
    val model =
      ReorderableListModel(listOf("a", "b"))
        .startDrag("a", pointerY = 20f)
        .moveTo("b", translationAdjustment = 0f)

    val updated =
      model.sync(
        incomingKeys = listOf("a", "b"),
        callerChanged = false,
        enabled = false,
      )

    assertEquals(listOf("a", "b"), updated.displayedKeys)
    assertNull(updated.drag)
  }
}
