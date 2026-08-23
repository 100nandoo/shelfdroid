package dev.halim.shelfdroid.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MyTextButton(
  modifier: Modifier = Modifier,
  text: String,
  enabled: Boolean = true,
  onClick: () -> Unit,
) {
  TextButton(
    modifier = modifier,
    enabled = enabled,
    onClick = onClick,
  ) {
    Text(text)
  }
}

@Composable
fun MyTextButtonRetry(
  modifier: Modifier = Modifier,
  message: String,
  onRetry: () -> Unit,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(message)
    MyTextButton(
      text = stringResource(R.string.retry),
      onClick = onRetry,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun MyTextButtonPreview() {
  PreviewWrapper { MyTextButton(text = "Text button", onClick = {}) }
}

@ShelfDroidPreview
@Composable
private fun MyTextButtonDisabledPreview() {
  PreviewWrapper { MyTextButton(text = "Disabled text button", enabled = false, onClick = {}) }
}

@ShelfDroidPreview
@Composable
private fun MyTextButtonRetryPreview() {
  PreviewWrapper {
    MyTextButtonRetry(
      message = "Unable to load metadata.",
      onRetry = {},
    )
  }
}
