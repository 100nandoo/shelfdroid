package dev.halim.shelfdroid.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ListItem(
  text: String,
  contentDescription: String,
  @DrawableRes icon: Int,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      painter = painterResource(id = icon),
      contentDescription = contentDescription,
      modifier = Modifier.size(24.dp),
    )
    Text(text, modifier = Modifier.padding(start = 12.dp).weight(1f))
  }
}

@ShelfDroidPreview
@Composable
private fun PreviewListItem() {
  PreviewWrapper(false) {
    Column {
      ListItem("Delete", "Delete", R.drawable.delete, {})
      ListItem("Edit", "Edit", R.drawable.delete, {})
    }
  }
}
