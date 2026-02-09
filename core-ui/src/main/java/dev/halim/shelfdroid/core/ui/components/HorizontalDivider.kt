package dev.halim.shelfdroid.core.ui.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable

@Composable
fun HorizontalDividerNoLast(size: Int, i: Int) {
  if (i != size - 1) {
    HorizontalDivider()
  }
}

@Composable
fun HorizontalDividerNoFirst(i: Int) {
  if (i != 0) {
    HorizontalDivider()
  }
}
