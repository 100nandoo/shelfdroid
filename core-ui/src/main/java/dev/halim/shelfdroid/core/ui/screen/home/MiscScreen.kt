package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun MiscScreen(
  isAdmin: Boolean = false,
  onSettingsClicked: () -> Unit = {},
  onListeningSessionClicked: () -> Unit = {},
  onOpenSessionClicked: () -> Unit = {},
  onUsersClicked: () -> Unit,
  onLibrariesClicked: () -> Unit,
  onApiKeysClicked: () -> Unit,
  onServerSettingsClicked: () -> Unit,
  onLogsClicked: () -> Unit,
  onBackupsClicked: () -> Unit,
) {
  MiscScreenContent(
    isAdmin = isAdmin,
    onSettingsClicked = onSettingsClicked,
    onListeningSessionClicked = onListeningSessionClicked,
    onOpenSessionClicked = onOpenSessionClicked,
    onUsersClicked = onUsersClicked,
    onLibrariesClicked = onLibrariesClicked,
    onApiKeysClicked = onApiKeysClicked,
    onServerSettingsClicked = onServerSettingsClicked,
    onLogsClicked = onLogsClicked,
    onBackupsClicked = onBackupsClicked,
  )
}

@Composable
private fun MiscScreenContent(
  isAdmin: Boolean = false,
  onOpenSessionClicked: () -> Unit = {},
  onListeningSessionClicked: () -> Unit = {},
  onSettingsClicked: () -> Unit = {},
  onUsersClicked: () -> Unit = {},
  onLibrariesClicked: () -> Unit = {},
  onApiKeysClicked: () -> Unit = {},
  onServerSettingsClicked: () -> Unit = {},
  onLogsClicked: () -> Unit = {},
  onBackupsClicked: () -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    if (isAdmin) {
      TextButton(
        onClick = onBackupsClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.backups))
      }

      TextButton(
        onClick = onLogsClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.logs))
      }

      TextButton(
        onClick = onServerSettingsClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.server_settings))
      }

      TextButton(
        onClick = onApiKeysClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.api_keys))
      }

      TextButton(
        onClick = onLibrariesClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.libraries))
      }

      TextButton(
        onClick = onUsersClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.users))
      }

      TextButton(
        onClick = onOpenSessionClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.open_sessions))
      }

      TextButton(
        onClick = onListeningSessionClicked,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      ) {
        Text(text = stringResource(R.string.listening_sessions))
      }
    }

    TextButton(
      onClick = onSettingsClicked,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
      Text(text = stringResource(R.string.settings))
    }
    Spacer(Modifier.height(16.dp))
  }
}

@ShelfDroidPreview
@Composable
fun MiscScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { MiscScreenContent() }
}

@ShelfDroidPreview
@Composable
fun MiscScreenContentAdminPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { MiscScreenContent(isAdmin = true) }
}
