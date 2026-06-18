package dev.halim.shelfdroid.helper

fun Long.formatFileSize(): String =
  when {
    this >= 1_000_000_000 -> "%.2f GB".format(this / 1_000_000_000.0)
    this >= 1_000_000 -> "%.2f MB".format(this / 1_000_000.0)
    this >= 1_000 -> "%.2f KB".format(this / 1_000.0)
    else -> "$this B"
  }
