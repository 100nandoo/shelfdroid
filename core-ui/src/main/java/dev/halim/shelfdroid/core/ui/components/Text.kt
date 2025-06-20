package dev.halim.shelfdroid.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun SettingsLabel(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.labelLarge,
) {
  Text(text = text, style = style, color = MaterialTheme.colorScheme.tertiary, modifier = modifier)
}

@Composable
fun SettingsBody(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
  Text(text = text, style = style, modifier = modifier)
}

@Composable
fun ExpandShrinkText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = 3,
  expanded: Boolean = false,
) {
  val expandedState = remember { mutableStateOf(expanded) }
  val textLinkColor = MaterialTheme.colorScheme.primary

  val linkStyles = remember {
    TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline, color = textLinkColor))
  }

  Column(modifier = modifier) {
    if (text.isNotEmpty()) {
      Text(
        modifier =
          Modifier.animateContentSize().clickable { expandedState.value = !expandedState.value },
        text = remember(text) { AnnotatedString.fromHtml(text, linkStyles = linkStyles) },
        maxLines = if (expandedState.value) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = if (expandedState.value) "Show less" else "Show more",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        modifier =
          Modifier.padding(top = 4.dp, bottom = 16.dp).clickable {
            expandedState.value = !expandedState.value
          },
      )
    }
  }
}

@Composable
fun AutoSizeText(
  modifier: Modifier = Modifier,
  text: String,
  maxLines: Int = 2,
  textAlign: TextAlign = TextAlign.Unspecified,
  style: TextStyle = LocalTextStyle.current,
  overflow: TextOverflow = TextOverflow.Ellipsis,
  color: Color = Color.Unspecified,
) {
  val textColor = color.takeOrElse { style.color.takeOrElse { LocalContentColor.current } }

  BasicText(
    text = text,
    modifier = modifier,
    autoSize = TextAutoSize.StepBased(maxFontSize = style.fontSize),
    maxLines = maxLines,
    overflow = overflow,
    style = style.merge(color = textColor, textAlign = textAlign),
  )
}
