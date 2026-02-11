package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ItemsPerPage
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USERNAME
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSIONS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

@Composable
fun ListeningSessionScreen(viewModel: ListeningSessionViewModel = hiltViewModel()) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ListeningSessionContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun ListeningSessionContent(
  uiState: ListeningSessionUiState = ListeningSessionUiState(),
  onEvent: (ListeningSessionEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    if (uiState.state is GenericState.Success) {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.Bottom,
        reverseLayout = true,
      ) {
        item { PageControl(uiState, onEvent) }
        items(uiState.sessions, key = { it.id }) { session ->
          HorizontalDivider()
          ListeningSessionItem(session)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageControl(
  uiState: ListeningSessionUiState,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  val totalPageIsNotZero = remember(uiState.pageInfo.numPages) { uiState.pageInfo.numPages != 0 }
  val goToPage =
    remember(uiState.pageInfo.inputPage, uiState.pageInfo.numPages) {
      {
        val value = uiState.pageInfo.inputPage.coerceIn(1, uiState.pageInfo.numPages)
        onEvent(ListeningSessionEvent.ChangeToPage(value - 1))
      }
    }

  Row(modifier = Modifier.padding(horizontal = 16.dp)) {
    ChipDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.users),
      options = uiState.users.mapNotNull { it.username },
      initialValue = uiState.pageInfo.selectedUser.username ?: ALL_USERNAME,
      onClick = { selected ->
        val user =
          uiState.users.firstOrNull { it.username == selected }
            ?: ListeningSessionUiState.User.ALL_USER
        onEvent(ListeningSessionEvent.FilterUser(user))
      },
    )

    Spacer(Modifier.width(8.dp))

    ChipDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.items),
      options = ItemsPerPage.entries.map { it.label.toString() },
      initialValue = uiState.listeningSessionPrefs.itemsPerPage.toString(),
      onClick = {
        onEvent(ListeningSessionEvent.ChangeItemsPerPage(ItemsPerPage.fromLabel(it.toInt())))
      },
    )
  }
  Row(
    modifier = Modifier.imePadding().padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
      modifier = Modifier.weight(1f).padding(bottom = 12.dp),
      enabled = totalPageIsNotZero,
      value = uiState.pageInfo.inputPage.toString(),
      maxLines = 1,
      label = { Text(stringResource(R.string.go_to_page)) },
      trailingIcon = {
        Icon(
          painter = painterResource(id = R.drawable.arrow_right),
          contentDescription = stringResource(R.string.go_to_page),
          modifier = Modifier.clickable(totalPageIsNotZero) { goToPage() },
        )
      },
      onValueChange = { text ->
        onEvent(ListeningSessionEvent.ChangeInputPage(text.toIntOrNull() ?: 1))
      },
      keyboardOptions =
        KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
      keyboardActions = KeyboardActions(onDone = { goToPage() }),
    )

    Spacer(Modifier.width(8.dp))

    PagePrevNextControl(Modifier.weight(1f), uiState, totalPageIsNotZero, onEvent)
  }
}

@Composable
private fun PagePrevNextControl(
  modifier: Modifier,
  uiState: ListeningSessionUiState,
  totalPageIsNotZero: Boolean,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  val text =
    if (totalPageIsNotZero) "${uiState.pageInfo.page + 1}/${uiState.pageInfo.numPages}" else "0"
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    MyIconButton(
      enabled = uiState.pageInfo.page != 0 && totalPageIsNotZero,
      painter = painterResource(R.drawable.chevron_left),
      contentDescription = stringResource(R.string.previous_page),
      onClick = { onEvent(ListeningSessionEvent.ChangePage(false)) },
      size = 48,
    )
    Spacer(Modifier.width(8.dp))
    Text(text)
    Spacer(Modifier.width(8.dp))
    MyIconButton(
      enabled = uiState.pageInfo.page < uiState.pageInfo.numPages - 1 && totalPageIsNotZero,
      painter = painterResource(R.drawable.chevron_right),
      contentDescription = stringResource(R.string.next_page),
      onClick = { onEvent(ListeningSessionEvent.ChangePage(true)) },
      size = 48,
    )
  }
}

@ShelfDroidPreview
@Composable
fun ListeningSessionScreenContentPreview() {
  val uiState = ListeningSessionUiState(state = GenericState.Success, sessions = LISTENING_SESSIONS)
  AnimatedPreviewWrapper(dynamicColor = false) { ListeningSessionContent(uiState = uiState) }
}
