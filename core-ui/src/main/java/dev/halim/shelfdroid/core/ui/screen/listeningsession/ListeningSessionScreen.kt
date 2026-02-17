@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.ItemsPerPage
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionApiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.Session
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState.User.Companion.ALL_USERNAME
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ChipDropdownMenu
import dev.halim.shelfdroid.core.ui.components.ListDeleteButton
import dev.halim.shelfdroid.core.ui.components.ListItemAction
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyIconButton
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.extensions.enable
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.LISTENING_SESSIONS
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun ListeningSessionScreen(
  viewModel: ListeningSessionViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  ListeningSessionContent(uiState = uiState, onEvent = viewModel::onEvent)
  val scope = rememberCoroutineScope()

  val deleteFailureMessage = stringResource(R.string.failed_to_delete_session)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is ListeningSessionApiState.DeleteFailure -> {
        scope.launch { snackbarHostState.showSnackbar(state.message ?: deleteFailureMessage) }
      }
      else -> Unit
    }
  }
}

@Composable
private fun ListeningSessionContent(
  uiState: ListeningSessionUiState = ListeningSessionUiState(),
  onEvent: (ListeningSessionEvent) -> Unit = {},
) {
  BackHandler(enabled = uiState.selection.isSelectionMode) {
    onEvent(ListeningSessionEvent.SelectionMode(false, ""))
  }
  val scope = rememberCoroutineScope()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var selectedSession by remember { mutableStateOf(Session("")) }

  val isDeleteDialogShown = remember { mutableStateOf(false) }
  var isFromSheet by remember { mutableStateOf(false) }

  ListeningSessionSheet(
    sheetState,
    selectedSession,
    {
      ListItemAction(
        text = stringResource(R.string.delete),
        contentDescription = stringResource(R.string.delete),
        icon = R.drawable.delete,
        onClick = {
          scope.launch { sheetState.hide() }
          isFromSheet = true
          isDeleteDialogShown.value = true
        },
      )
    },
  )

  val count = remember(uiState.selection.selectedIds) { uiState.selection.selectedIds.size }

  DeleteDialog(
    isDeleteDialogShown,
    count,
    isFromSheet,
    { onEvent(ListeningSessionEvent.DeleteSession(selectedSession)) },
    { onEvent(ListeningSessionEvent.DeleteSessions) },
  )

  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      item(key = "control") {
        AnimatedVisibility(uiState.selection.isSelectionMode.not()) {
          Control(Modifier.animateItem(), uiState.userAndCountFilter, uiState.pageInfo, onEvent)
        }
      }
      items(uiState.sessions, key = { it.id }) { session ->
        HorizontalDivider()
        val isSelected = session.id in uiState.selection.selectedIds
        ListeningSessionItem(
          session,
          true,
          isSelected,
          uiState.selection.isSelectionMode,
          { onEvent(ListeningSessionEvent.Select(session.id)) },
          { onEvent(ListeningSessionEvent.SelectionMode(true, session.id)) },
          {
            selectedSession = session
            scope.launch { sheetState.show() }
          },
        )
      }
    }
    AnimatedVisibility(uiState.selection.isSelectionMode) {
      ListDeleteButton(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        count = count,
        noneText = R.string.no_sessions_selected,
        typeText = R.plurals.plurals_session,
        onClick = {
          isFromSheet = false
          isDeleteDialogShown.value = true
        },
      )
    }
  }
}

@Composable
private fun DeleteDialog(
  showDeleteDialog: MutableState<Boolean>,
  count: Int,
  fromSheet: Boolean,
  onDeleteOne: () -> Unit,
  onDeleteMultiple: () -> Unit,
) {
  val isMultiple = count > 1
  val type =
    if (isMultiple) "$count ${pluralStringResource(R.plurals.plurals_session, count)}"
    else pluralStringResource(R.plurals.plurals_session, count)
  val text = pluralStringResource(R.plurals.plurals_delete_warning, count, type)
  MyAlertDialog(
    title = stringResource(R.string.delete),
    text = text,
    showDialog = showDeleteDialog.value,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      if (fromSheet.not()) onDeleteMultiple() else onDeleteOne()
      showDeleteDialog.value = false
    },
    onDismiss = { showDeleteDialog.value = false },
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Control(
  modifier: Modifier = Modifier,
  userAndCountFilter: ListeningSessionUiState.UserAndCountFilter,
  pageInfo: ListeningSessionUiState.PageInfo,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  Column(modifier.imePadding().padding(top = 16.dp)) {
    PageControl(pageInfo, onEvent)
    UserAndCountFilter(userAndCountFilter, onEvent)
  }
}

@Composable
private fun PageControl(
  pageInfo: ListeningSessionUiState.PageInfo,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  val totalPageAboveTwo = remember(pageInfo.numPages) { pageInfo.numPages > 2 }
  val goToPage =
    remember(pageInfo.inputPage, pageInfo.numPages) {
      {
        val value = pageInfo.inputPage.coerceIn(1, pageInfo.numPages)
        onEvent(ListeningSessionEvent.ChangeToPage(value - 1))
      }
    }
  Row(
    modifier = Modifier.padding(horizontal = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    OutlinedTextField(
      modifier = Modifier.weight(1f).padding(bottom = 12.dp),
      enabled = totalPageAboveTwo,
      value = pageInfo.inputPage.toString(),
      maxLines = 1,
      label = { Text(stringResource(R.string.go_to_page)) },
      trailingIcon = {
        Icon(
          painter = painterResource(id = R.drawable.arrow_right),
          contentDescription = stringResource(R.string.go_to_page),
          modifier = Modifier.clickable(totalPageAboveTwo) { goToPage() },
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

    PagePrevNextControl(Modifier.weight(1f), pageInfo, totalPageAboveTwo, onEvent)
  }
}

@Composable
private fun UserAndCountFilter(
  userAndCountFilter: ListeningSessionUiState.UserAndCountFilter,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  Row(modifier = Modifier.padding(horizontal = 16.dp)) {
    ChipDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.users_semicolon),
      options = userAndCountFilter.users.mapNotNull { it.username },
      initialValue = userAndCountFilter.selectedUser.username ?: ALL_USERNAME,
      onClick = { selected ->
        val user =
          userAndCountFilter.users.firstOrNull { it.username == selected }
            ?: ListeningSessionUiState.User.ALL_USER
        onEvent(ListeningSessionEvent.FilterUser(user))
      },
    )

    Spacer(Modifier.width(8.dp))

    ChipDropdownMenu(
      modifier = Modifier.weight(1f),
      label = stringResource(R.string.items),
      options = ItemsPerPage.entries.map { it.label.toString() },
      initialValue = userAndCountFilter.itemsPerPage.toString(),
      onClick = {
        onEvent(ListeningSessionEvent.ChangeItemsPerPage(ItemsPerPage.fromLabel(it.toInt())))
      },
    )
  }
}

@Composable
private fun PagePrevNextControl(
  modifier: Modifier,
  pageInfo: ListeningSessionUiState.PageInfo,
  totalPageAboveTwo: Boolean,
  onEvent: (ListeningSessionEvent) -> Unit,
) {
  val text =
    if (totalPageAboveTwo) "${pageInfo.page + 1}/${pageInfo.numPages}" else "${pageInfo.numPages}"
  Row(
    modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center,
  ) {
    MyIconButton(
      enabled = pageInfo.page != 0 && totalPageAboveTwo,
      painter = painterResource(R.drawable.chevron_left),
      contentDescription = stringResource(R.string.previous_page),
      onClick = { onEvent(ListeningSessionEvent.ChangePage(false)) },
      size = 48,
    )
    Spacer(Modifier.width(8.dp))
    Text(text, color = MaterialTheme.colorScheme.onSurface.enable(totalPageAboveTwo))
    Spacer(Modifier.width(8.dp))
    MyIconButton(
      enabled = pageInfo.page < pageInfo.numPages - 1 && totalPageAboveTwo,
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
