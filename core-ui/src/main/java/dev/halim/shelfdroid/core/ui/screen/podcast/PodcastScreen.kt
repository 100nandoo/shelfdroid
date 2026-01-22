package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastApiState
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.InitMediaControllerIfMainActivity
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.MyAlertDialogWithCheckbox
import dev.halim.shelfdroid.core.ui.components.VisibilityCircular
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.item.ItemDetail
import kotlinx.coroutines.launch

@Composable
fun PodcastScreen(
  viewModel: PodcastViewModel = hiltViewModel(),
  playerViewModel: PlayerViewModel,
  snackbarHostState: SnackbarHostState,
  onEpisodeClicked: (String, String) -> Unit,
  onFetchEpisodeSuccess: (String) -> Unit,
) {
  InitMediaControllerIfMainActivity()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is PodcastApiState.AddSuccess -> {
        viewModel.onEvent(PodcastEvent.ResetAddEpisodeState)
        onFetchEpisodeSuccess(viewModel.id)
      }
      is PodcastApiState.AddFailure -> {
        scope.launch { snackbarHostState.showSnackbar(state.message) }
      }
      is PodcastApiState.DeleteFailure -> {
        scope.launch { snackbarHostState.showSnackbar(state.message) }
        viewModel.onEvent(PodcastEvent.SelectionMode(false, ""))
      }
      is PodcastApiState.DeleteSuccess -> {
        val message =
          context.resources.getQuantityString(
            R.plurals.success_delete_episode_count,
            state.size,
            state.size,
          )
        scope.launch { snackbarHostState.showSnackbar(message) }
        viewModel.onEvent(PodcastEvent.SelectionMode(false, ""))
      }
      else -> Unit
    }
  }

  if (uiState.state == GenericState.Success) {
    PodcastScreenContent(
      uiState = uiState,
      id = viewModel.id,
      snackbarHostState = snackbarHostState,
      onEvent = viewModel::onEvent,
      onEpisodeClicked = onEpisodeClicked,
      onPlayClicked = { itemId, episodeId, isDownloaded ->
        playerViewModel.onEvent(PlayerEvent.PlayPodcast(itemId, episodeId, isDownloaded))
      },
    )
  }
}

@Composable
fun PodcastScreenContent(
  uiState: PodcastUiState =
    PodcastUiState(
      state = GenericState.Success,
      title = Defaults.TITLE,
      author = Defaults.AUTHOR_NAME,
      cover = Defaults.IMAGE_URL,
      description = Defaults.DESCRIPTION,
      episodes = Defaults.EPISODES,
      isSelectionMode = true,
    ),
  id: String = Defaults.BOOK_ID,
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
  onEvent: (PodcastEvent) -> Unit = {},
  onEpisodeClicked: (String, String) -> Unit = { _, _ -> },
  onPlayClicked: (String, String, Boolean) -> Unit = { _, _, _ -> },
) {
  BackHandler(enabled = uiState.isSelectionMode) { onEvent(PodcastEvent.SelectionMode(false, "")) }
  val isDownloaded = uiState.prefs.displayPrefs.filter.isDownloaded()
  val episodes =
    if (isDownloaded) uiState.episodes.filter { it.download.state.isDownloaded() }
    else uiState.episodes
  val count = uiState.selectedEpisodeIds.size

  val lazyListState = rememberLazyListState()
  val fabVisible by remember { derivedStateOf { lazyListState.isScrollInProgress.not() } }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      state = lazyListState,
      modifier = Modifier.weight(1f).mySharedBound(Animations.containerKey(id)),
      reverseLayout = true,
    ) {
      item { Spacer(modifier = Modifier.height(12.dp)) }
      items(episodes) { episode ->
        val isSelected = uiState.selectedEpisodeIds.contains(episode.episodeId)
        HorizontalDivider()
        EpisodeItem(
          id,
          episode,
          isSelected,
          uiState.canDeleteEpisode,
          onEvent,
          onEpisodeClicked,
          onPlayClicked,
          snackbarHostState,
          uiState.isSelectionMode,
        )
      }
      item {
        Header(uiState.canAddEpisode, uiState.apiState, onEvent)
        Spacer(modifier = Modifier.height(16.dp))

        ExpandShrinkText(Modifier.padding(horizontal = 16.dp), uiState.description)

        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(horizontal = 16.dp),
        ) {
          ItemDetail(id, uiState.cover, uiState.title, uiState.author)
        }
      }
    }
    AnimatedVisibility(visible = uiState.isSelectionMode && fabVisible) {
      DeleteSection(
        count,
        uiState.prefs.crudPrefs.episodeHardDelete,
        uiState.prefs.crudPrefs.episodeAutoSelectFinished,
        onDeleteClick = { onEvent(PodcastEvent.DeleteEpisode(it)) },
        onAutoSelectFinishedChange = { onEvent(PodcastEvent.SwitchAutoSelectFinished(it)) },
      )
    }
  }
}

@Composable
private fun Header(
  canAddEpisode: Boolean,
  state: PodcastApiState,
  onEvent: (PodcastEvent) -> Unit,
) {
  Row(
    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      modifier = Modifier.weight(1f),
      text = stringResource(R.string.episodes),
      style = MaterialTheme.typography.headlineMedium,
      textAlign = TextAlign.Left,
    )

    if (canAddEpisode) {
      VisibilityCircular(state == PodcastApiState.AddLoading) {
        Icon(
          painter = painterResource(id = R.drawable.search),
          contentDescription = stringResource(R.string.search_podcast),
          modifier = Modifier.clickable { onEvent(PodcastEvent.AddEpisode) },
        )
      }
    }
  }
}

@Composable
private fun DeleteSection(
  count: Int,
  initialHardDelete: Boolean,
  autoSelectFinished: Boolean = false,
  onDeleteClick: (Boolean) -> Unit,
  onAutoSelectFinishedChange: (Boolean) -> Unit,
) {
  var showDeleteDialog by remember { mutableStateOf(false) }
  var hardDelete by remember { mutableStateOf(initialHardDelete) }

  val isZero = count == 0
  val text =
    if (isZero) stringResource(R.string.no_episodes_selected)
    else pluralStringResource(id = R.plurals.delete_episode_count, count = count, count)

  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Start,
    ) {
      Checkbox(checked = autoSelectFinished, onCheckedChange = onAutoSelectFinishedChange)
      Spacer(modifier = Modifier.width(8.dp))
      Text(text = stringResource(R.string.auto_select_finished_episodes))
    }
    Row {
      Button(
        onClick = { showDeleteDialog = true },
        modifier = Modifier.weight(1f),
        enabled = count > 0,
      ) {
        Icon(
          painter = painterResource(R.drawable.delete),
          contentDescription = stringResource(R.string.delete),
          modifier = Modifier.padding(end = 8.dp),
        )
        Text(text)
      }
    }
  }

  val dialogText =
    if (count == 1) stringResource(R.string.dialog_delete_episode)
    else stringResource(R.string.dialog_delete_episodes)
  MyAlertDialogWithCheckbox(
    title = stringResource(R.string.delete),
    text = dialogText,
    showDialog = showDeleteDialog,
    confirmText = stringResource(R.string.delete),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      onDeleteClick(hardDelete)
      showDeleteDialog = false
    },
    onDismiss = { showDeleteDialog = false },
    checkboxChecked = hardDelete,
    onCheckboxChange = { hardDelete = it },
    checkboxText = stringResource(R.string.delete_from_file_system),
  )
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { PodcastScreenContent() }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { PodcastScreenContent() }
}
