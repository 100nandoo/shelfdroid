@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.podcast

import android.content.ClipData
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
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
import dev.halim.shelfdroid.core.ui.components.CheckboxRow
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.ListDeleteButton
import dev.halim.shelfdroid.core.ui.components.MyAlertDialogWithCheckbox
import dev.halim.shelfdroid.core.ui.components.VisibilityCircular
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.navigation.Podcast
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.item.ItemDetail
import dev.halim.shelfdroid.core.ui.screen.rssfeeds.ItemGeneratedRssFeedSheet
import kotlinx.coroutines.launch

@Composable
fun PodcastScreen(
  navKey: Podcast,
  viewModel: PodcastViewModel =
    hiltViewModel<PodcastViewModel, PodcastViewModel.Factory> { factory ->
      factory.create(navKey)
    },
  playerController: PlayerController,
  snackbarHostState: SnackbarHostState,
  onEpisodeClicked: (String, String) -> Unit,
  onEditEpisodeClicked: (String, String) -> Unit,
  onFetchEpisodeSuccess: (String) -> Unit,
) {
  InitMediaControllerIfMainActivity()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val context = LocalContext.current
  val clipboardManager = LocalClipboard.current
  val publicFeedLabel = stringResource(R.string.public_feed_url)
  val copiedMessage = stringResource(R.string.rss_url_copied)
  val openSuccessMessage = stringResource(R.string.generated_rss_feed_opened)
  val closeSuccessMessage = stringResource(R.string.rss_feed_closed)
  val openFailureMessage = stringResource(R.string.failed_to_open_generated_rss_feed)
  val closeFailureMessage = stringResource(R.string.failed_to_close_rss_feed)
  var showRssFeedSheet by rememberSaveable { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is PodcastApiState.AddSuccess -> {
        viewModel.onEvent(PodcastEvent.ResetApiState)
        onFetchEpisodeSuccess(viewModel.id)
      }
      is PodcastApiState.AddFailure -> {
        scope.launch { snackbarHostState.showErrorSnackbar(state.message) }
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      is PodcastApiState.DeleteFailure -> {
        scope.launch { snackbarHostState.showErrorSnackbar(state.message) }
        viewModel.onEvent(PodcastEvent.SelectionMode(false, ""))
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      is PodcastApiState.DeleteSuccess -> {
        val message =
          context.resources.getQuantityString(
            R.plurals.success_delete_episode_count,
            state.size,
            state.size,
          )
        scope.launch { snackbarHostState.showSuccessSnackbar(message) }
        viewModel.onEvent(PodcastEvent.SelectionMode(false, ""))
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      is PodcastApiState.OpenRssFeedFailure -> {
        scope.launch {
          snackbarHostState.showErrorSnackbar(state.message.ifBlank { openFailureMessage })
        }
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      PodcastApiState.OpenRssFeedSuccess -> {
        scope.launch { snackbarHostState.showSuccessSnackbar(openSuccessMessage) }
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      is PodcastApiState.CloseRssFeedFailure -> {
        scope.launch {
          snackbarHostState.showErrorSnackbar(state.message.ifBlank { closeFailureMessage })
        }
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      PodcastApiState.CloseRssFeedSuccess -> {
        showRssFeedSheet = false
        scope.launch { snackbarHostState.showSuccessSnackbar(closeSuccessMessage) }
        viewModel.onEvent(PodcastEvent.ResetApiState)
      }
      else -> Unit
    }
  }

  if (showRssFeedSheet && uiState.generatedRssFeed.isVisible) {
    ItemGeneratedRssFeedSheet(
      sheetState = sheetState,
      title = uiState.title,
      rssFeed = uiState.generatedRssFeed,
      isProcessing =
        uiState.apiState == PodcastApiState.OpenRssFeedLoading ||
          uiState.apiState == PodcastApiState.CloseRssFeedLoading,
      onDismiss = { showRssFeedSheet = false },
      onCopyUrl = { value ->
        scope.launch {
          clipboardManager.setClipEntry(ClipData.newPlainText(publicFeedLabel, value).toClipEntry())
          snackbarHostState.showSuccessSnackbar(copiedMessage)
        }
      },
      onOpenFeed = { slug, preventIndexing, ownerName, ownerEmail ->
        viewModel.onEvent(
          PodcastEvent.OpenGeneratedRssFeed(
            slug = slug,
            preventIndexing = preventIndexing,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
          )
        )
      },
      onCloseFeed = { feedId -> viewModel.onEvent(PodcastEvent.CloseGeneratedRssFeed(feedId)) },
      onShowMessage = { message ->
        scope.launch { snackbarHostState.showErrorSnackbar(message) }
      },
    )
  }

  if (uiState.state == GenericState.Success) {
    PodcastScreenContent(
      uiState = uiState,
      id = viewModel.id,
      snackbarHostState = snackbarHostState,
      onEvent = viewModel::onEvent,
      onGeneratedRssFeedClicked = {
        showRssFeedSheet = true
        scope.launch { sheetState.show() }
      },
      onEpisodeClicked = onEpisodeClicked,
      onEditEpisodeClicked = onEditEpisodeClicked,
      onPlayClicked = { itemId, episodeId, _ ->
        playerController.onEvent(PlayerEvent.PlayPodcast(itemId, episodeId))
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
  onGeneratedRssFeedClicked: () -> Unit = {},
  onEpisodeClicked: (String, String) -> Unit = { _, _ -> },
  onEditEpisodeClicked: (String, String) -> Unit = { _, _ -> },
  onPlayClicked: (String, String, Boolean) -> Unit = { _, _, _ -> },
) {
  BackHandler(enabled = uiState.isSelectionMode) { onEvent(PodcastEvent.SelectionMode(false, "")) }
  val isDownloaded = uiState.prefs.displayPrefs.filter.isDownloaded()
  val episodes =
    if (isDownloaded) uiState.episodes.filter { it.download.state.isDownloaded() }
    else uiState.episodes
  val count = uiState.selectedEpisodeIds.size
  val selectedActionEpisode = uiState.episodes.find { it.episodeId == uiState.actionSheetEpisodeId }

  Column(modifier = Modifier.fillMaxSize()) {
    LazyColumn(
      modifier = Modifier.weight(1f).mySharedBound(Animations.containerKey(id)),
      reverseLayout = true,
    ) {
      item { Spacer(modifier = Modifier.height(12.dp)) }
      items(episodes) { episode ->
        val isSelected = uiState.selectedEpisodeIds.contains(episode.episodeId)
        HorizontalDivider()
        EpisodeItem(
          id,
          uiState.title,
          episode,
          isSelected,
          uiState.canEditEpisode || uiState.canDeleteEpisode,
          onEvent,
          onEpisodeClicked,
          onPlayClicked,
          snackbarHostState,
          uiState.isSelectionMode,
        )
      }
      item {
        Header(
          canAddEpisode = uiState.canAddEpisode,
          showGeneratedRssFeed = uiState.generatedRssFeed.isVisible,
          state = uiState.apiState,
          onGeneratedRssFeedClicked = onGeneratedRssFeedClicked,
          onEvent = onEvent,
        )
        Spacer(modifier = Modifier.height(16.dp))

        ExpandShrinkText(Modifier.padding(horizontal = 16.dp), uiState.description)

        ItemDetail(
          id,
          uiState.cover,
          uiState.title,
          uiState.author,
          modifier = Modifier.padding(horizontal = 16.dp),
        )
      }
    }
    AnimatedVisibility(uiState.isSelectionMode) {
      DeleteSection(
        count = count,
        initialHardDelete = uiState.prefs.crudPrefs.episodeHardDelete,
        autoSelectFinished = uiState.prefs.crudPrefs.episodeAutoSelectFinished,
        onDeleteClick = { onEvent(PodcastEvent.DeleteEpisode(it)) },
        onAutoSelectFinishedChange = { onEvent(PodcastEvent.SwitchAutoSelectFinished(it)) },
      )
    }
  }

  selectedActionEpisode?.let { episode ->
    EpisodeActionSheet(
      title = episode.title,
      podcastTitle = uiState.title,
      publishedAt = episode.publishedAt,
      canEdit = uiState.canEditEpisode,
      canDelete = uiState.canDeleteEpisode,
      onDismiss = { onEvent(PodcastEvent.DismissEpisodeActions) },
      onEdit = {
        onEvent(PodcastEvent.DismissEpisodeActions)
        onEditEpisodeClicked(id, episode.episodeId)
      },
      onDelete = { onEvent(PodcastEvent.StartDeleteSelection(episode.episodeId)) },
    )
  }
}

@Composable
private fun Header(
  canAddEpisode: Boolean,
  showGeneratedRssFeed: Boolean,
  state: PodcastApiState,
  onGeneratedRssFeedClicked: () -> Unit,
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

    if (showGeneratedRssFeed) {
      Icon(
        painter = painterResource(id = R.drawable.rss),
        contentDescription = stringResource(R.string.generated_rss_feed),
        modifier = Modifier.padding(end = 12.dp).clickable(onClick = onGeneratedRssFeedClicked),
      )
    }

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
  initialShowDeleteDialog: Boolean = false,
  autoSelectFinished: Boolean = false,
  onDeleteClick: (Boolean) -> Unit,
  onAutoSelectFinishedChange: (Boolean) -> Unit,
) {
  var showDeleteDialog by
    remember(initialShowDeleteDialog) { mutableStateOf(initialShowDeleteDialog) }
  var hardDelete by remember { mutableStateOf(initialHardDelete) }

  Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    CheckboxRow(
      modifier = Modifier.fillMaxWidth(),
      checked = autoSelectFinished,
      text = stringResource(R.string.auto_select_finished_episodes),
      onCheckedChange = onAutoSelectFinishedChange,
      wholeRowClickable = true,
    )
    ListDeleteButton(
      modifier = Modifier.fillMaxWidth(),
      count = count,
      noneText = R.string.no_episodes_selected,
      typeText = R.plurals.plurals_episode,
      onClick = { showDeleteDialog = true },
    )
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

@ShelfDroidPreview
@Composable
private fun DeleteSectionDialogPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    DeleteSection(
      count = 2,
      initialHardDelete = true,
      initialShowDeleteDialog = true,
      autoSelectFinished = true,
      onDeleteClick = {},
      onAutoSelectFinishedChange = {},
    )
  }
}
