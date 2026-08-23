package dev.halim.shelfdroid.core.ui.screen.metadata

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTonalIconButton
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun GenreTagItem(
  label: String,
  enabled: Boolean,
  @StringRes renameContentDescriptionResId: Int,
  @StringRes deleteContentDescriptionResId: Int,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
    Text(
      label,
      Modifier.weight(1f).padding(vertical = 12.dp),
    )
    MyTonalIconButton(
      painterResId = R.drawable.edit,
      contentDescriptionResId = renameContentDescriptionResId,
      enabled = enabled,
      onClick = onRename,
    )
    MyTonalIconButton(
      painterResId = R.drawable.delete,
      contentDescriptionResId = deleteContentDescriptionResId,
      enabled = enabled,
      onClick = onDelete,
    )
  }
}

@ShelfDroidPreview
@Composable
private fun GenreTagItemPreview() {
  PreviewWrapper {
    GenreTagItem(
      label = "Adventure",
      enabled = true,
      renameContentDescriptionResId = R.string.rename_tag_action,
      deleteContentDescriptionResId = R.string.delete_tag_action,
      onRename = {},
      onDelete = {},
    )
  }
}
