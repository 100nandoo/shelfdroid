package dev.halim.shelfdroid.core.ui.screen.listeningsession

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.listeningsession.ListeningSessionUiState
import dev.halim.shelfdroid.core.ui.R
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ListeningSessionContent(
  uiState: ListeningSessionUiState = ListeningSessionUiState(),
  onEvent: (ListeningSessionEvent) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(uiState.state is GenericState.Loading) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    val page =
      remember(uiState.pageInfo.page) { mutableStateOf((uiState.pageInfo.page + 1).toString()) }
    val pageChange = remember { mutableStateOf(false) }
    val commitPage = {
      pageChange.value = false
      val pageInt = page.value.toIntOrNull() ?: 1
      val value = pageInt.coerceIn(1, uiState.pageInfo.numPages)
      page.value = value.toString()
      onEvent(ListeningSessionEvent.ChangeToPage(value - 1))
    }
    val isKeyboardHidden = WindowInsets.isImeVisible.not()
    if (isKeyboardHidden && pageChange.value) {
      commitPage()
    }
    LazyColumn(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      item {
        Row(
          modifier = Modifier.imePadding().padding(vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          MyIconButton(
            enabled = uiState.pageInfo.page != 0,
            painter = painterResource(R.drawable.chevron_left),
            contentDescription = stringResource(R.string.add_podcast),
            onClick = { onEvent(ListeningSessionEvent.ChangePage(false)) },
            size = 48,
          )
          Spacer(Modifier.width(8.dp))
          OutlinedTextField(
            modifier = Modifier.widthIn(min = 32.dp, max = 104.dp),
            enabled = true,
            value = page.value,
            maxLines = 1,
            suffix = {
              Text(
                text = "/${uiState.pageInfo.numPages}",
                color = OutlinedTextFieldDefaults.colors().disabledTextColor,
              )
            },
            onValueChange = { text ->
              page.value = text
              pageChange.value = true
            },
            keyboardOptions =
              KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commitPage() }),
          )
          Spacer(Modifier.width(8.dp))
          MyIconButton(
            enabled = uiState.pageInfo.page != uiState.pageInfo.numPages - 1,
            painter = painterResource(R.drawable.chevron_right),
            contentDescription = stringResource(R.string.add_podcast),
            onClick = { onEvent(ListeningSessionEvent.ChangePage(true)) },
            size = 48,
          )
        }
      }
      itemsIndexed(uiState.sessions, key = { i, it -> it.id }) { i, session ->
        HorizontalDivider()
        ListeningSessionItem(session)
      }
      item { Spacer(modifier = Modifier.height(16.dp)) }
    }
  }
}

@ShelfDroidPreview
@Composable
fun ListeningSessionScreenContentPreview() {
  val uiState = ListeningSessionUiState(state = GenericState.Success, sessions = LISTENING_SESSIONS)
  AnimatedPreviewWrapper(dynamicColor = false) { ListeningSessionContent(uiState = uiState) }
}
