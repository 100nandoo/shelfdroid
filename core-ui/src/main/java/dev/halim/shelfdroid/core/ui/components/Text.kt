package dev.halim.shelfdroid.core.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

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
          Modifier.animateContentSize().clickable(role = Role.Button) {
            expandedState.value = !expandedState.value
          },
        text = remember(text) { AnnotatedString.fromHtml(text, linkStyles = linkStyles) },
        maxLines = if (expandedState.value) Int.MAX_VALUE else maxLines,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text =
          if (expandedState.value) stringResource(R.string.show_less)
          else stringResource(R.string.show_more),
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

@Composable
fun TextHeadlineLarge(modifier: Modifier = Modifier, text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.headlineLarge,
    modifier = modifier.padding(bottom = 4.dp),
  )
}

@Composable
fun TextHeadlineMedium(modifier: Modifier = Modifier, text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.headlineMedium,
    modifier = modifier.padding(bottom = 4.dp),
  )
}

@Composable
fun TextHeadlineSmall(modifier: Modifier = Modifier, text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.headlineSmall,
    modifier = modifier.padding(bottom = 4.dp),
  )
}

@Composable
fun TextTitleLarge(modifier: Modifier = Modifier, text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleLarge,
    modifier = modifier.padding(bottom = 4.dp),
  )
}

@Composable
fun TextTitleMedium(modifier: Modifier = Modifier, text: String, color: Color = Color.Unspecified) {
  Text(
    text = text,
    color = color,
    style = MaterialTheme.typography.titleMedium,
    modifier = modifier.padding(bottom = 4.dp),
  )
}

@Composable
fun TextTitleSmall(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.titleSmall,
    modifier = modifier.padding(bottom = 4.dp),
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextBodyLarge(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyLarge,
    modifier = modifier,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextBodyMedium(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodyMedium,
    modifier = modifier,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextBodySmall(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    text = text,
    style = MaterialTheme.typography.bodySmall,
    modifier = modifier,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextLabelLarge(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    modifier = modifier,
    text = text,
    style = MaterialTheme.typography.labelLarge,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextLabelMedium(
  modifier: Modifier = Modifier,
  text: AnnotatedString,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    modifier = modifier,
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextLabelMedium(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    modifier = modifier,
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextLabelSmall(
  modifier: Modifier = Modifier,
  text: String,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
) {
  Text(
    modifier = modifier,
    text = text,
    style = MaterialTheme.typography.labelSmall,
    color = color,
    maxLines = maxLines,
    overflow = overflow,
  )
}

@Composable
fun TextLabelValue(
  label: String,
  value: String,
  modifier: Modifier = Modifier,
  color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
  labelWeight: Float = 1f,
  valueWeight: Float = 3f,
) {
  if (value.isNotEmpty()) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
      TextLabelSmall(text = label, color = color, modifier = Modifier.weight(labelWeight))
      TextLabelSmall(text = ": ", color = color)
      TextLabelSmall(modifier = Modifier.weight(valueWeight), text = value, color = color)
    }
  }
}

private const val PreviewExpandableText =
  "<p>ShelfDroid keeps previews close to the composables they exercise so UI review stays fast.</p>"

@ShelfDroidPreview
@Composable
private fun TextHelpersPreview() {
  PreviewWrapper(dynamicColor = false) {
    Column(modifier = Modifier.padding(16.dp)) {
      TextHeadlineSmall(text = "Headline small")
      TextTitleLarge(text = "Title large")
      TextTitleMedium(text = "Title medium")
      TextTitleSmall(
        text = "Title small with a longer line that should truncate cleanly",
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      TextBodyLarge(text = "Body large text")
      TextBodyMedium(text = "Body medium text")
      TextBodySmall(text = "Body small text")
      TextLabelLarge(text = "Label large")
      TextLabelMedium(text = "Label medium")
      TextLabelSmall(text = "Label small")
      Row(verticalAlignment = Alignment.CenterVertically) {
        TextBodyMedium(text = "Auto size text:")
        AutoSizeText(
          modifier = Modifier.padding(start = 8.dp),
          text = "Audiobookshelf server backup restored successfully",
          maxLines = 1,
          style = MaterialTheme.typography.titleMedium,
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun ExpandShrinkTextPreview() {
  PreviewWrapper(dynamicColor = false) {
    ExpandShrinkText(
      modifier = Modifier.padding(16.dp),
      text = PreviewExpandableText,
      expanded = true,
    )
  }
}
