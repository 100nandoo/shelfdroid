package dev.halim.shelfdroid.core.utils

fun Float.toPercent(): String {
  return "${(this * 100).toInt()}%"
}
