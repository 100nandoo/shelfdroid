package dev.halim.shelfdroid.core.ui.screen.settings.listeningsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ItemsPerPage
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USERNAME
import dev.halim.shelfdroid.core.data.screen.settings.listeningsession.SettingsListeningSessionUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.MySegmentedButton
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun SettingsListeningSessionScreen(viewModel: SettingsListeningSessionViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  SettingsListeningSessionContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun SettingsListeningSessionContent(
  uiState: SettingsListeningSessionUiState = SettingsListeningSessionUiState(),
  onEvent: (SettingsListeningSessionEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    val defaultUser =
      uiState.users.firstOrNull { uiState.listeningSessionPrefs.defaultUserId == it.id }
        ?: User.ALL_USER
    ChipDropdownMenu(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
      label = stringResource(R.string.default_user),
      options = uiState.users.mapNotNull { it.username },
      initialValue = defaultUser.username ?: ALL_USERNAME,
      onClick = { selected ->
        val user = uiState.users.firstOrNull { it.username == selected } ?: User.ALL_USER
        onEvent(SettingsListeningSessionEvent.DefaultUser(user.id))
      },
    )

    Spacer(Modifier.height(8.dp))

    MySegmentedButton(
      modifier = Modifier.padding(16.dp),
      label = stringResource(R.string.default_items_per_page),
      options = ItemsPerPage.entries.map { it.label.toString() },
      selectedValue = uiState.listeningSessionPrefs.itemsPerPage.toString(),
      onClick = {
        onEvent(
          SettingsListeningSessionEvent.ChangeItemsPerPage(ItemsPerPage.fromLabel(it.toInt()))
        )
      },
    )

    Spacer(modifier = Modifier.height(12.dp))
  }
}

@ShelfDroidPreview
@Composable
fun SettingsListeningSessionScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { SettingsListeningSessionContent() }
}
