package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T, K : Any> ReorderHandle(
  state: ReorderableLazyListState<T, K>,
  itemKey: K,
  contentDescription: String,
  moveUpLabel: String,
  moveDownLabel: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  visible: Boolean = true,
) {
  if (!visible) return
  val interactionEnabled = state.canInteract(itemKey, enabled)
  val currentState by rememberUpdatedState(state)
  val currentItemKey by rememberUpdatedState(itemKey)
  val dragModifier =
    if (interactionEnabled) {
      Modifier.pointerInput(Unit) {
        detectDragGestures(
          orientationLock = Orientation.Vertical,
          onDragStart = { _, _, _ -> currentState.startDrag(currentItemKey) },
          onDragEnd = { currentState.endDrag() },
          onDragCancel = { currentState.cancelDrag() },
          shouldAwaitTouchSlop = { false },
          onDrag = { change, dragAmount ->
            change.consume()
            currentState.dragBy(dragAmount.y)
          },
        )
      }
    } else {
      Modifier
    }
  val accessibilityActions = buildList {
    if (interactionEnabled && state.canMoveUp(itemKey)) {
      add(
        CustomAccessibilityAction(moveUpLabel) {
          currentState.moveByAccessibility(
            currentItemKey,
            currentState.indexOf(currentItemKey) - 1,
          )
        }
      )
    }
    if (interactionEnabled && state.canMoveDown(itemKey)) {
      add(
        CustomAccessibilityAction(moveDownLabel) {
          currentState.moveByAccessibility(
            currentItemKey,
            currentState.indexOf(currentItemKey) + 1,
          )
        }
      )
    }
  }

  Box(
    modifier =
      modifier
        .size(48.dp)
        .alpha(if (interactionEnabled) 1f else 0.38f)
        .then(dragModifier)
        .semantics(mergeDescendants = true) {
          this.contentDescription = contentDescription
          customActions = accessibilityActions
          if (!interactionEnabled) disabled()
        },
    contentAlignment = Alignment.Center,
  ) {
    Icon(painter = painterResource(R.drawable.drag_handle), contentDescription = null)
  }
}

@ShelfDroidPreview
@Composable
private fun ReorderHandlePreview() {
  PreviewWrapper {
    val state =
      rememberReorderableLazyListState(
        items = listOf("Books"),
        key = { it },
        lazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
        onMove = { _, _ -> },
      )
    ReorderHandle(
      state = state,
      itemKey = "Books",
      contentDescription = "Reorder Books, position 1 of 1",
      moveUpLabel = "Move Books up",
      moveDownLabel = "Move Books down",
    )
  }
}
