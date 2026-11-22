package dev.halim.shelfdroid.core.ui.screen.settings.search

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsSearchResultCountItem(count: Int, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.settings_search_result_count, count),
    modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@ShelfDroidPreview
@Composable
fun SettingsSearchResultCountItemPreview() {
  PreviewWrapper(dynamicColor = false) { SettingsSearchResultCountItem(count = 4) }
}
