package dev.halim.shelfdroid.core.ui.extensions

inline fun <R> String.letNotBlank(block: (String) -> R): R? =
  if (isNotBlank()) block(this) else null
