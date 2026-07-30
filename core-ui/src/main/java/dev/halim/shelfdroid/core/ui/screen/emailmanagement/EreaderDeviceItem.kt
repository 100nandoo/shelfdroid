package dev.halim.shelfdroid.core.ui.screen.emailmanagement

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.emailmanagement.EreaderDeviceItem as EreaderDeviceUiItem
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTonalIconButton
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
internal fun EreaderDeviceItem(
  device: EreaderDeviceUiItem,
  accessibleBy: String,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onEditClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      TextTitleSmall(text = device.name)
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp),
        text = device.email,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      TextLabelSmall(
        modifier = Modifier.padding(top = 4.dp),
        text = stringResource(R.string.accessible_by_value, accessibleBy),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Row {
      MyTonalIconButton(
        painterResId = R.drawable.edit,
        contentDescriptionResId = R.string.edit_device,
        onClick = onEditClick,
      )
      MyTonalIconButton(
        painterResId = R.drawable.delete,
        contentDescriptionResId = R.string.delete_device,
        onClick = onDeleteClick,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
private fun EreaderDeviceItemPreview() {
  PreviewWrapper(dynamicColor = false) {
    Column {
      HorizontalDivider()
      EreaderDeviceItem(
        device =
          EreaderDeviceUiItem(
            name = "Kindle",
            email = "kindle@example.com",
          ),
        accessibleBy = "cross",
        onEditClick = {},
        onDeleteClick = {},
      )
      HorizontalDivider()
    }
  }
}
