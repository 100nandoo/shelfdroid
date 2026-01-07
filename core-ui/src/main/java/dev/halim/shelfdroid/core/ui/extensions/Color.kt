package dev.halim.shelfdroid.core.ui.extensions

import androidx.compose.ui.graphics.Color

fun Color.enable(enabled: Boolean): Color = this.copy(alpha = if (enabled) 1f else 0.38f)
