package dev.halim.shelfdroid.core.ui.screen.usersettings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.usersettings.UserSettingsUiState.User
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.extensions.enable
import dev.halim.shelfdroid.core.ui.preview.Defaults.USER_SETTINGS_USERS
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun LazyItemScope.UserSettingsItem(
  user: User,
  onInfoClicked: () -> Unit = {},
  onDeleteClicked: () -> Unit = {},
  onClicked: () -> Unit = {},
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  MyAlertDialog(
    title = stringResource(R.string.delete),
    text = stringResource(R.string.dialog_delete_user),
    showDialog = showDeleteDialog,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onDeleteClicked()
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
  )

  Row(
    modifier =
      Modifier.animateItem()
        .fillMaxWidth()
        .clickable { onClicked() }
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(modifier = Modifier.weight(3f).padding(end = 16.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          user.username + " ∙ ",
          style = MaterialTheme.typography.titleSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurface.enable(user.isActive),
        )
        Text(
          user.type.name.lowercase(),
          style = MaterialTheme.typography.labelSmall,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          color = MaterialTheme.colorScheme.onSurfaceVariant.enable(user.isActive),
        )
      }
      if (user.lastSession.title.isNotBlank()) {
        Text(
          user.lastSession.title,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.enable(user.isActive),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      if (user.lastSession.title.isNotBlank()) {
        Text(
          user.lastSeen,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.enable(user.isActive),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }

    FilledTonalIconButton(onClick = onInfoClicked) {
      Icon(
        painter = painterResource(id = R.drawable.info),
        contentDescription = stringResource(R.string.info),
      )
    }
    val isDeleteVisible = user.type.isRoot().not()
    if (isDeleteVisible)
      FilledTonalIconButton(onClick = { showDeleteDialog = true }) {
        Icon(
          painter = painterResource(id = R.drawable.delete),
          contentDescription = stringResource(R.string.delete),
        )
      }
  }
}

@ShelfDroidPreview
@Composable
fun UserSettingsItemPreview() {
  PreviewWrapper {
    LazyColumn { items(USER_SETTINGS_USERS, key = { it.id }) { UserSettingsItem(it) } }
  }
}
