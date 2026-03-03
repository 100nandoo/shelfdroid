package dev.halim.shelfdroid.core.ui.extensions

import androidx.compose.ui.text.AnnotatedString.Builder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

inline fun <R : Any> Builder.withBold(block: Builder.() -> R): R {
  val index = pushStyle(style = SpanStyle(fontWeight = FontWeight.Bold))
  return try {
    block(this)
  } finally {
    pop(index)
  }
}

fun Builder.appendTwoLine() {
  this.append("\n\n")
}

fun Builder.appendBold(text: String) {
  this.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text) }
}
