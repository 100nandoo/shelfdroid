package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import be.digitalia.compose.htmlconverter.htmlToAnnotatedString

@Composable
fun SettingsLabel(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelLarge,
) {
    Text(
        text = text,
        style = style,
        color = MaterialTheme.colorScheme.tertiary,
        modifier = modifier
    )
}

@Composable
fun SettingsBody(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Text(
        text = text,
        style = style,
        modifier = modifier
    )
}

@Composable
fun ExpandShrinkText(text: String, maxLines: Int = 3) {
    val expandedState = remember { mutableStateOf(false) }
    Column {
        Text(
            text = remember(text) { htmlToAnnotatedString(text) },
            maxLines = if (expandedState.value) Int.MAX_VALUE else maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable { expandedState.value = !expandedState.value }
        )
        if (text.isNotEmpty()) {
            Text(
                text = if (expandedState.value) "Show less" else "Show more",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 4.dp, bottom = 16.dp)
                    .clickable { expandedState.value = !expandedState.value }
            )
        }
    }
}