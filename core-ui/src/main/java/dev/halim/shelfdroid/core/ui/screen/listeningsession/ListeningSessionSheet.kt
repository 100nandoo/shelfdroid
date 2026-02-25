@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Device
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.PlayerInfo
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextLabelValue
import dev.halim.shelfdroid.core.ui.components.TextTitleMedium
import dev.halim.shelfdroid.core.ui.components.VisibilityUp
import dev.halim.shelfdroid.core.ui.components.orchestrators.LibraryItemHeader
import dev.halim.shelfdroid.core.ui.extensions.letNotBlank
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSION
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import kotlinx.coroutines.launch

@Composable
fun ListeningSessionSheet(
  sheetState: SheetState,
  session: Session,
  listItemAction: @Composable () -> Unit,
) {
  val scope = rememberCoroutineScope()

  VisibilityUp(sheetState.isVisible) {
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = { scope.launch { sheetState.hide() } },
    ) {
      Column(Modifier.padding(bottom = 64.dp, start = 16.dp, end = 16.dp)) {
        LibraryItemHeader(
          Modifier,
          session.id,
          session.item.title,
          session.item.author,
          session.item.cover,
        )

        HorizontalDivider(Modifier.padding(bottom = 16.dp))

        DevicePlayerSection(session.device, session.playerInfo)
        Spacer(Modifier.height(16.dp))

        SessionTimeSection(session.sessionTime, session.user.username)
        Spacer(Modifier.height(16.dp))

        listItemAction()
      }
    }
  }
}

private val paddingBottom = Modifier.padding(bottom = 2.dp)

@Composable
private fun DevicePlayerSection(device: Device, playerInfo: PlayerInfo) {
  Row {
    Column(Modifier.weight(1f)) {
      TextTitleMedium(text = "Device")
      device.device?.let { TextLabelSmall(text = it, modifier = paddingBottom) }
      device.client?.let { TextLabelSmall(text = it, modifier = paddingBottom) }
      device.browser?.let { TextLabelSmall(text = it, modifier = paddingBottom) }
      device.ip?.let { TextLabelSmall(text = it, modifier = paddingBottom) }
    }
    Column(Modifier.weight(1f)) {
      TextTitleMedium(text = "Media Player")
      playerInfo.player.letNotBlank { TextLabelSmall(text = it, modifier = paddingBottom) }
      playerInfo.method.letNotBlank { TextLabelSmall(text = it, modifier = paddingBottom) }
    }
  }
}

@Composable
fun SessionTimeSection(sessionTime: ListeningSessionUiState.SessionTime, username: String?) {
  TextTitleMedium(text = "Details")
  username?.let { TextLabelValue("User", username, modifier = paddingBottom) }
  TextLabelValue("Started at", sessionTime.startedAt, modifier = paddingBottom)
  TextLabelValue("Updated at", sessionTime.updatedAt, modifier = paddingBottom)
  TextLabelValue("Start time", sessionTime.startTime, modifier = paddingBottom)
  TextLabelValue("Last time", sessionTime.lastTime, modifier = paddingBottom)
  TextLabelValue("Duration", sessionTime.duration, modifier = paddingBottom)
}

@ShelfDroidPreview
@Composable
private fun ListeningSessionSheetPreview() {
  AnimatedPreviewWrapper(false) {
    val density = LocalDensity.current
    val state = sheetState(density)

    ListeningSessionSheet(state, LISTENING_SESSION, {})
    LaunchedEffect(Unit) { state.show() }
  }
}
