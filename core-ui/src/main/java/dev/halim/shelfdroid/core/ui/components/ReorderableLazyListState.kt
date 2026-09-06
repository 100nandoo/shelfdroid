package dev.halim.shelfdroid.core.ui.components

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive

private const val NANOSECONDS_PER_SECOND = 1_000_000_000f

@Stable
internal class ReorderableLazyListState<T, K : Any>
internal constructor(
  val lazyListState: LazyListState,
  initialItems: List<T>,
  initialEnabled: Boolean,
  private var keyOf: (T) -> K,
  private var onMove: (K, Int) -> Unit,
  private var edgeScrollZonePx: Float,
  private var maximumEdgeScrollSpeedPx: Float,
  internal var draggedScale: Float,
  internal var draggedElevationPx: Float,
) {
  private var callerItems = initialItems
  private var enabled = initialEnabled
  private var model by mutableStateOf(ReorderableListModel(initialItems.map(keyOf)))

  var items by mutableStateOf(initialItems)
    private set

  val draggedKey: K?
    get() = model.drag?.key

  internal val draggedTranslationY: Float
    get() = model.drag?.translationY ?: 0f

  fun isDragging(key: K): Boolean = draggedKey == key

  fun indexOf(key: K): Int = model.displayedKeys.indexOf(key)

  fun canMoveUp(key: K): Boolean = indexOf(key) > 0

  fun canMoveDown(key: K): Boolean {
    val index = indexOf(key)
    return index >= 0 && index < model.displayedKeys.lastIndex
  }

  fun canInteract(key: K, itemEnabled: Boolean): Boolean =
    enabled && itemEnabled && (draggedKey == null || draggedKey == key)

  internal fun update(
    callerItems: List<T>,
    enabled: Boolean,
    keyOf: (T) -> K,
    onMove: (K, Int) -> Unit,
    edgeScrollZonePx: Float,
    maximumEdgeScrollSpeedPx: Float,
    draggedScale: Float,
    draggedElevationPx: Float,
  ) {
    this.keyOf = keyOf
    this.onMove = onMove
    this.edgeScrollZonePx = edgeScrollZonePx
    this.maximumEdgeScrollSpeedPx = maximumEdgeScrollSpeedPx
    this.draggedScale = draggedScale
    this.draggedElevationPx = draggedElevationPx

    val callerChanged = callerItems != this.callerItems
    this.callerItems = callerItems
    this.enabled = enabled
    model = model.sync(callerItems.map(keyOf), callerChanged, enabled)
    refreshItems()
  }

  fun resetToCallerItems() {
    model = ReorderableListModel(callerItems.map(keyOf))
    refreshItems()
  }

  fun startDrag(key: K) {
    if (!enabled || model.drag != null) return
    val itemInfo = lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key } ?: return
    model = model.startDrag(key, itemInfo.offset + itemInfo.size / 2f)
  }

  fun dragBy(deltaY: Float) {
    if (model.drag == null) return
    model = model.dragBy(deltaY)
    val movedDrag = model.drag ?: return
    val currentInfo =
      lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == movedDrag.key } ?: return
    val draggedCenter = currentInfo.offset + movedDrag.translationY + currentInfo.size / 2f
    val targetKey =
      findReorderTarget(
        orderedKeys = model.displayedKeys,
        currentKey = movedDrag.key,
        draggedCenter = draggedCenter,
        visibleItems =
          lazyListState.layoutInfo.visibleItemsInfo.map { item ->
            ReorderableItemBounds(item.key, item.offset, item.size)
          },
      ) ?: return
    val targetInfo =
      lazyListState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == targetKey } ?: return
    val targetIndex = model.displayedKeys.indexOf(targetKey)
    val newBaseOffset =
      if (targetIndex > movedDrag.currentIndex) {
        targetInfo.offset + targetInfo.size - currentInfo.size
      } else {
        targetInfo.offset
      }
    model = model.moveTo(targetKey, (currentInfo.offset - newBaseOffset).toFloat())
    refreshItems()
  }

  fun endDrag() {
    val result = model.finishDrag()
    model = result.model
    refreshItems()
    result.move?.let { move -> onMove(move.key, move.destinationIndex) }
  }

  fun cancelDrag() {
    model = model.cancelDrag()
    refreshItems()
  }

  fun moveByAccessibility(key: K, destinationIndex: Int): Boolean {
    if (!canInteract(key, itemEnabled = true)) return false
    val result = model.moveImmediately(key, destinationIndex)
    if (result.move == null) return false
    model = result.model
    refreshItems()
    onMove(result.move.key, result.move.destinationIndex)
    return true
  }

  internal suspend fun autoScroll() {
    var previousFrameNanos = 0L
    while (kotlin.coroutines.coroutineContext.isActive && model.drag != null) {
      val frameNanos = withFrameNanos { it }
      val drag = model.drag ?: continue
      val layoutInfo = lazyListState.layoutInfo
      val scrollSpeed =
        when {
          drag.pointerY < layoutInfo.viewportStartOffset + edgeScrollZonePx ->
            -maximumEdgeScrollSpeedPx *
              ((layoutInfo.viewportStartOffset + edgeScrollZonePx - drag.pointerY) /
                  edgeScrollZonePx)
                .coerceIn(0f, 1f)
          drag.pointerY > layoutInfo.viewportEndOffset - edgeScrollZonePx ->
            maximumEdgeScrollSpeedPx *
              ((drag.pointerY - (layoutInfo.viewportEndOffset - edgeScrollZonePx)) /
                  edgeScrollZonePx)
                .coerceIn(0f, 1f)
          else -> 0f
        }
      if (previousFrameNanos != 0L && scrollSpeed != 0f) {
        val elapsedSeconds = (frameNanos - previousFrameNanos) / NANOSECONDS_PER_SECOND
        val consumed = lazyListState.scrollBy(scrollSpeed * elapsedSeconds)
        model = model.offsetDrag(consumed)
        dragBy(0f)
      }
      previousFrameNanos = frameNanos
    }
  }

  private fun refreshItems() {
    val itemsByKey = callerItems.associateBy(keyOf)
    val reorderedItems = model.displayedKeys.mapNotNull(itemsByKey::get)
    items = if (reorderedItems.size == callerItems.size) reorderedItems else callerItems
  }
}

@Composable
internal fun <T, K : Any> rememberReorderableLazyListState(
  items: List<T>,
  key: (T) -> K,
  lazyListState: LazyListState,
  enabled: Boolean = true,
  edgeScrollZone: Dp = 72.dp,
  maximumEdgeScrollSpeed: Dp = 1_200.dp,
  draggedScale: Float = 1.02f,
  draggedElevation: Dp = 8.dp,
  onMove: (K, Int) -> Unit,
): ReorderableLazyListState<T, K> {
  val density = LocalDensity.current
  val edgeScrollZonePx = with(density) { edgeScrollZone.toPx() }
  val maximumEdgeScrollSpeedPx = with(density) { maximumEdgeScrollSpeed.toPx() }
  val draggedElevationPx = with(density) { draggedElevation.toPx() }
  val state =
    remember(lazyListState) {
      ReorderableLazyListState(
        lazyListState = lazyListState,
        initialItems = items,
        initialEnabled = enabled,
        keyOf = key,
        onMove = onMove,
        edgeScrollZonePx = edgeScrollZonePx,
        maximumEdgeScrollSpeedPx = maximumEdgeScrollSpeedPx,
        draggedScale = draggedScale,
        draggedElevationPx = draggedElevationPx,
      )
    }
  state.update(
    callerItems = items,
    enabled = enabled,
    keyOf = key,
    onMove = onMove,
    edgeScrollZonePx = edgeScrollZonePx,
    maximumEdgeScrollSpeedPx = maximumEdgeScrollSpeedPx,
    draggedScale = draggedScale,
    draggedElevationPx = draggedElevationPx,
  )
  LaunchedEffect(state.draggedKey) { state.autoScroll() }
  return state
}

@SuppressLint("ModifierFactoryExtensionFunction")
internal fun <T, K : Any> LazyItemScope.reorderableItemModifier(
  state: ReorderableLazyListState<T, K>,
  key: K,
): Modifier =
  if (state.isDragging(key)) {
    Modifier.zIndex(1f).graphicsLayer {
      translationY = state.draggedTranslationY
      scaleX = state.draggedScale
      scaleY = state.draggedScale
      shadowElevation = state.draggedElevationPx
    }
  } else {
    Modifier.animateItem()
  }

internal data class ReorderableItemBounds(
  val key: Any,
  val offset: Int,
  val size: Int,
)

internal fun <K : Any> findReorderTarget(
  orderedKeys: List<K>,
  currentKey: K,
  draggedCenter: Float,
  visibleItems: List<ReorderableItemBounds>,
): K? {
  val currentIndex = orderedKeys.indexOf(currentKey)
  if (currentIndex < 0) return null
  val currentInfo = visibleItems.firstOrNull { it.key == currentKey } ?: return null
  val currentCenter = currentInfo.offset + currentInfo.size / 2f
  val indexedItems = visibleItems.mapNotNull { item ->
    val index = orderedKeys.indexOfFirst { key -> key == item.key }
    if (index < 0) null else index to item
  }
  val target =
    if (draggedCenter > currentCenter) {
      indexedItems
        .filter { (index, item) ->
          index > currentIndex && draggedCenter > item.offset + item.size / 2f
        }
        .maxByOrNull { it.first }
    } else {
      indexedItems
        .filter { (index, item) ->
          index < currentIndex && draggedCenter < item.offset + item.size / 2f
        }
        .minByOrNull { it.first }
    }
  return target?.let { (index) -> orderedKeys[index] }
}

internal data class ReorderMove<K : Any>(val key: K, val destinationIndex: Int)

internal data class ReorderResult<K : Any>(
  val model: ReorderableListModel<K>,
  val move: ReorderMove<K>?,
)

internal data class ReorderDrag<K : Any>(
  val key: K,
  val sourceKeys: List<K>,
  val originalIndex: Int,
  val currentIndex: Int,
  val translationY: Float = 0f,
  val pointerY: Float,
)

internal data class PendingReorder<K : Any>(
  val sourceKeys: List<K>,
  val expectedKeys: List<K>,
)

internal data class ReorderableListModel<K : Any>(
  val displayedKeys: List<K>,
  val drag: ReorderDrag<K>? = null,
  val pendingReorder: PendingReorder<K>? = null,
) {
  fun sync(
    incomingKeys: List<K>,
    callerChanged: Boolean,
    enabled: Boolean,
  ): ReorderableListModel<K> {
    if (!enabled || (drag != null && callerChanged)) return ReorderableListModel(incomingKeys)
    if (!callerChanged) return this
    val pending = pendingReorder ?: return ReorderableListModel(incomingKeys)
    return when (incomingKeys) {
      pending.sourceKeys -> copy(displayedKeys = pending.expectedKeys)
      pending.expectedKeys -> ReorderableListModel(incomingKeys)
      else -> ReorderableListModel(incomingKeys)
    }
  }

  fun startDrag(key: K, pointerY: Float): ReorderableListModel<K> {
    val index = displayedKeys.indexOf(key)
    if (index < 0 || drag != null) return this
    return copy(
      drag =
        ReorderDrag(
          key = key,
          sourceKeys = displayedKeys,
          originalIndex = index,
          currentIndex = index,
          pointerY = pointerY,
        ),
      pendingReorder = null,
    )
  }

  fun dragBy(deltaY: Float): ReorderableListModel<K> {
    val current = drag ?: return this
    return copy(
      drag =
        current.copy(
          translationY = current.translationY + deltaY,
          pointerY = current.pointerY + deltaY,
        )
    )
  }

  fun offsetDrag(deltaY: Float): ReorderableListModel<K> {
    val current = drag ?: return this
    return copy(drag = current.copy(translationY = current.translationY + deltaY))
  }

  fun moveTo(targetKey: K, translationAdjustment: Float): ReorderableListModel<K> {
    val current = drag ?: return this
    val targetIndex = displayedKeys.indexOf(targetKey)
    if (targetIndex < 0 || targetIndex == current.currentIndex) return this
    val reordered = displayedKeys.toMutableList()
    reordered.add(targetIndex, reordered.removeAt(current.currentIndex))
    return copy(
      displayedKeys = reordered,
      drag =
        current.copy(
          currentIndex = targetIndex,
          translationY = current.translationY + translationAdjustment,
        ),
    )
  }

  fun finishDrag(): ReorderResult<K> {
    val completed = drag ?: return ReorderResult(this, null)
    if (completed.currentIndex == completed.originalIndex) {
      return ReorderResult(ReorderableListModel(completed.sourceKeys), null)
    }
    return ReorderResult(
      model =
        copy(
          drag = null,
          pendingReorder = PendingReorder(completed.sourceKeys, displayedKeys),
        ),
      move = ReorderMove(completed.key, completed.currentIndex),
    )
  }

  fun cancelDrag(): ReorderableListModel<K> {
    val cancelled = drag ?: return this
    return ReorderableListModel(cancelled.sourceKeys)
  }

  fun moveImmediately(key: K, destinationIndex: Int): ReorderResult<K> {
    if (drag != null) return ReorderResult(this, null)
    val sourceIndex = displayedKeys.indexOf(key)
    val destination = destinationIndex.coerceIn(0, displayedKeys.lastIndex)
    if (sourceIndex < 0 || sourceIndex == destination) return ReorderResult(this, null)
    val reordered = displayedKeys.toMutableList()
    reordered.add(destination, reordered.removeAt(sourceIndex))
    return ReorderResult(
      model =
        copy(
          displayedKeys = reordered,
          pendingReorder = PendingReorder(displayedKeys, reordered),
        ),
      move = ReorderMove(key, destination),
    )
  }
}
