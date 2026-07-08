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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.TextHeadlineSmall
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
  val modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    if (isAdmin) {
      TextHeadlineSmall(
        modifier.padding(top = 16.dp),
        text = stringResource(R.string.server),
        textAlign = TextAlign.Center,
      )
      TextButton(
        onClick = onBackupsClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.backups))
      }

      TextButton(
        onClick = onLogsClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.logs))
      }

      TextButton(
        onClick = onApiKeysClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.api_keys))
      }

      TextButton(
        onClick = onUsersClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.users))
      }

      TextButton(
        onClick = onServerSettingsClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.settings))
      }

      TextButton(
        enabled = false,
        onClick = onLibrariesClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.libraries))
      }

      TextButton(
        onClick = onOpenSessionClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.open_sessions))
      }

      TextButton(
        onClick = onListeningSessionClicked,
        modifier = modifier,
      ) {
        Text(text = stringResource(R.string.listening_sessions))
      }
    }
    TextHeadlineSmall(
      modifier.padding(top = 32.dp),
      text = stringResource(R.string.client),
      textAlign = TextAlign.Center,
    )
    TextButton(
      onClick = onSettingsClicked,
      modifier = modifier,
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
