package dev.halim.shelfdroid.core.ui.screen.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.LogLevel
import dev.halim.shelfdroid.core.data.screen.logs.LogsUiState
import dev.halim.shelfdroid.core.ui.components.TextLabelMedium
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleLarge
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LOG_HOUR_HEADER
import dev.halim.shelfdroid.core.ui.preview.Defaults.LOG_LOG
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LogItem(log: LogsUiState.LogItem.Log, modifier: Modifier = Modifier) {
  Row(
    modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    TextLabelMedium(
      text = buildLogMessage(log.message),
      modifier = Modifier.weight(1f),
      maxLines = 2,
    )
    Column(horizontalAlignment = Alignment.End) {
      LogLevelText(log.level)
      log.time?.let { Time(it) }
    }
  }
}

@Composable
fun HourHeader(hourHeader: LogsUiState.LogItem.HourHeader, modifier: Modifier = Modifier) {
  Column(modifier = modifier) {
    TextTitleLarge(
      text = hourHeader.hour,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
    )
    HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
  }
}

@Composable
private fun LogLevelText(logLevel: LogLevel) {
  when (logLevel) {
    LogLevel.DEBUG -> TextLabelSmall(text = "D", color = MaterialTheme.colorScheme.outline)
    LogLevel.INFO -> TextLabelSmall(text = "I", color = MaterialTheme.colorScheme.primary)
    LogLevel.WARNING -> TextLabelSmall(text = "W", color = MaterialTheme.colorScheme.tertiary)
    LogLevel.ERROR -> TextLabelSmall(text = "E", color = MaterialTheme.colorScheme.error)
  }
}

@Composable
private fun buildLogMessage(message: String): AnnotatedString {
  val primary = MaterialTheme.colorScheme.primary
  val tertiary = MaterialTheme.colorScheme.tertiary
  val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

  return buildAnnotatedString {
    val tagRegex = "\\[.*?\\]".toRegex()
    val quoteRegex = "\".*?\"".toRegex()

    var currentIndex = 0

    val matches =
      (tagRegex.findAll(message) + quoteRegex.findAll(message)).sortedBy { it.range.first }

    matches.forEach { match ->
      // Normal text before match
      if (currentIndex < match.range.first) {
        append(message.substring(currentIndex, match.range.first))
      }

      val color =
        when {
          match.value.startsWith("[") -> primary
          match.value.startsWith("\"") -> tertiary
          else -> onSurfaceVariant
        }

      withStyle(style = SpanStyle(color = color)) { append(match.value) }

      currentIndex = match.range.last + 1
    }

    // Remaining text
    if (currentIndex < message.length) {
      append(message.substring(currentIndex))
    }
  }
}

@Composable
private fun Time(time: String) {
  TextLabelSmall(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@ShelfDroidPreview
@Composable
fun LogItemPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    Column {
      HourHeader(LOG_HOUR_HEADER)
      LogItem(LOG_LOG)
    }
  }
}
