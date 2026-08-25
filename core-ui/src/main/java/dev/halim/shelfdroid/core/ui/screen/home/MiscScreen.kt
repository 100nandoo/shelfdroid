package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.MyTextButton
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
  onEmailManagementClicked: () -> Unit,
  onAppriseNotificationSettingsClicked: () -> Unit,
  onRssFeedsClicked: () -> Unit,
  onLogsClicked: () -> Unit,
  onBackupsClicked: () -> Unit,
  onMetadataUtilitiesClicked: () -> Unit = {},
  onAuthenticationSettingsClicked: () -> Unit = {},
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
    onAuthenticationSettingsClicked = onAuthenticationSettingsClicked,
    onEmailManagementClicked = onEmailManagementClicked,
    onAppriseNotificationSettingsClicked = onAppriseNotificationSettingsClicked,
    onRssFeedsClicked = onRssFeedsClicked,
    onLogsClicked = onLogsClicked,
    onBackupsClicked = onBackupsClicked,
    onMetadataUtilitiesClicked = onMetadataUtilitiesClicked,
  )
}

fun shouldShowAuthenticationSettings(isAdmin: Boolean): Boolean = isAdmin

fun shouldShowLibraryItemMetadataUtilities(isAdmin: Boolean): Boolean = isAdmin

fun shouldShowLibraryAdministration(isAdmin: Boolean): Boolean = isAdmin

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
  onAuthenticationSettingsClicked: () -> Unit = {},
  onEmailManagementClicked: () -> Unit = {},
  onAppriseNotificationSettingsClicked: () -> Unit = {},
  onRssFeedsClicked: () -> Unit = {},
  onLogsClicked: () -> Unit = {},
  onBackupsClicked: () -> Unit = {},
  onMetadataUtilitiesClicked: () -> Unit = {},
) {
  val modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    if (shouldShowAuthenticationSettings(isAdmin)) {
      TextHeadlineSmall(
        modifier.padding(top = 16.dp),
        text = stringResource(R.string.server),
        textAlign = TextAlign.Center,
      )

      MyTextButton(
        onClick = onRssFeedsClicked,
        modifier = modifier,
        text = stringResource(R.string.rss_feeds),
      )

      MyTextButton(
        onClick = onBackupsClicked,
        modifier = modifier,
        text = stringResource(R.string.backups),
      )

      MyTextButton(
        onClick = onLogsClicked,
        modifier = modifier,
        text = stringResource(R.string.logs),
      )

      MyTextButton(
        onClick = onApiKeysClicked,
        modifier = modifier,
        text = stringResource(R.string.api_keys),
      )

      MyTextButton(
        onClick = onUsersClicked,
        modifier = modifier,
        text = stringResource(R.string.users),
      )

      MyTextButton(
        onClick = onServerSettingsClicked,
        modifier = modifier,
        text = stringResource(R.string.settings),
      )

      MyTextButton(
        onClick = onAuthenticationSettingsClicked,
        modifier = modifier,
        text = stringResource(R.string.authentication_settings),
      )

      MyTextButton(
        onClick = onEmailManagementClicked,
        modifier = modifier,
        text = stringResource(R.string.email_management),
      )

      MyTextButton(
        onClick = onAppriseNotificationSettingsClicked,
        modifier = modifier,
        text = stringResource(R.string.apprise_notification_settings),
      )

      MyTextButton(
        onClick = onMetadataUtilitiesClicked,
        modifier = modifier,
        text = stringResource(R.string.library_item_metadata_utilities),
      )

      MyTextButton(
        enabled = shouldShowLibraryAdministration(isAdmin),
        onClick = onLibrariesClicked,
        modifier = modifier,
        text = stringResource(R.string.libraries),
      )

      MyTextButton(
        onClick = onOpenSessionClicked,
        modifier = modifier,
        text = stringResource(R.string.open_sessions),
      )

      MyTextButton(
        onClick = onListeningSessionClicked,
        modifier = modifier,
        text = stringResource(R.string.listening_sessions),
      )
    }
    TextHeadlineSmall(
      modifier.padding(top = 32.dp),
      text = stringResource(R.string.client),
      textAlign = TextAlign.Center,
    )
    MyTextButton(
      onClick = onSettingsClicked,
      modifier = modifier,
      text = stringResource(R.string.settings),
    )
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
