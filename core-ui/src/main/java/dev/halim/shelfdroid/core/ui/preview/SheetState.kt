@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.preview

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

fun sheetState(density: Density): SheetState {
  return SheetState(
    skipPartiallyExpanded = true,
    initialValue = SheetValue.Expanded,
    positionalThreshold = { with(density) { 56.dp.toPx() } },
    velocityThreshold = { with(density) { 125.dp.toPx() } },
  )
}
