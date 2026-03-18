package dev.halim.shelfdroid.core.ui.extensions

import java.util.Locale

inline fun <R> String.letNotBlank(block: (String) -> R): R? =
  if (isNotBlank()) block(this) else null

fun String.capitalized(): String {
  return this.lowercase().replaceFirstChar { it.titlecase(Locale.getDefault()) }
}
