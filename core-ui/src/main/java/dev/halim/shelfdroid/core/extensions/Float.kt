package dev.halim.shelfdroid.core.extensions

import java.math.RoundingMode

fun Float.toSpeedText(): String {
  return this.toBigDecimal().setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
}
