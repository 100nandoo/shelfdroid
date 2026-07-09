@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.book

import android.content.ClipData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.DownloadState
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.book.BookApiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.InitMediaControllerIfMainActivity
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.PlayDownloadAndEdit
import dev.halim.shelfdroid.core.ui.components.TextLabelValue
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.navigation.Book
import dev.halim.shelfdroid.core.ui.player.PlayerController
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.forItemAction
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.item.ItemDetail
import dev.halim.shelfdroid.core.ui.screen.rssfeeds.ItemGeneratedRssFeedSheet
import dev.halim.shelfdroid.media.service.PlayerStore
import kotlinx.coroutines.launch

@Composable
fun BookScreen(
  navKey: Book,
  viewModel: BookViewModel =
    hiltViewModel<BookViewModel, BookViewModel.Factory> { factory -> factory.create(navKey) },
  playerStore: PlayerStore,
  playerController: PlayerController,
  snackbarHostState: SnackbarHostState,
  onEditClicked: (String) -> Unit = {},
) {
  InitMediaControllerIfMainActivity()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerUiState by playerStore.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboard.current
  val publicFeedLabel = stringResource(R.string.public_feed_url)
  val copiedMessage = stringResource(R.string.rss_url_copied)
  val openSuccessMessage = stringResource(R.string.generated_rss_feed_opened)
  val closeSuccessMessage = stringResource(R.string.rss_feed_closed)
  val openFailureMessage = stringResource(R.string.failed_to_open_generated_rss_feed)
  val closeFailureMessage = stringResource(R.string.failed_to_close_rss_feed)
  var showRssFeedSheet by rememberSaveable { mutableStateOf(false) }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val downloadState = if (uiState.isSingleTrack) uiState.download.state else uiState.downloads.state

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is BookApiState.OpenRssFeedFailure -> {
        scope.launch {
          snackbarHostState.showErrorSnackbar(state.message.ifBlank { openFailureMessage })
        }
        viewModel.onEvent(BookEvent.ResetApiState)
      }

      BookApiState.OpenRssFeedSuccess -> {
        scope.launch { snackbarHostState.showSuccessSnackbar(openSuccessMessage) }
        viewModel.onEvent(BookEvent.ResetApiState)
      }

      is BookApiState.CloseRssFeedFailure -> {
        scope.launch {
          snackbarHostState.showErrorSnackbar(state.message.ifBlank { closeFailureMessage })
        }
        viewModel.onEvent(BookEvent.ResetApiState)
      }

      BookApiState.CloseRssFeedSuccess -> {
        showRssFeedSheet = false
        scope.launch { snackbarHostState.showSuccessSnackbar(closeSuccessMessage) }
        viewModel.onEvent(BookEvent.ResetApiState)
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
        uiState.apiState == BookApiState.OpenRssFeedLoading ||
          uiState.apiState == BookApiState.CloseRssFeedLoading,
      onDismiss = { showRssFeedSheet = false },
      onCopyUrl = { value ->
        scope.launch {
          clipboardManager.setClipEntry(ClipData.newPlainText(publicFeedLabel, value).toClipEntry())
          snackbarHostState.showSuccessSnackbar(copiedMessage)
        }
      },
      onOpenFeed = { slug, preventIndexing, ownerName, ownerEmail ->
        viewModel.onEvent(
          BookEvent.OpenGeneratedRssFeed(
            slug = slug,
            preventIndexing = preventIndexing,
            ownerName = ownerName,
            ownerEmail = ownerEmail,
          )
        )
      },
      onCloseFeed = { feedId -> viewModel.onEvent(BookEvent.CloseGeneratedRssFeed(feedId)) },
      onShowMessage = { message ->
        scope.launch { snackbarHostState.showErrorSnackbar(message) }
      },
    )
  }

  if (uiState.state == GenericState.Success) {
    BookScreenContent(
      id = viewModel.id,
      cover = uiState.cover,
      title = uiState.title,
      author = uiState.author,
      description = uiState.description,
      subtitle = uiState.subtitle,
      duration = uiState.duration,
      remaining = uiState.remaining,
      narrator = uiState.narrator,
      publishYear = uiState.publishYear,
      publisher = uiState.publisher,
      genres = uiState.genres,
      language = uiState.language,
      progress = uiState.progress,
      canEdit = uiState.canEdit,
      isEbook = uiState.isEbook,
      downloadState = downloadState,
      playPause = playerUiState.playPause.forItemAction(playerUiState.id == viewModel.id),
      generatedRssFeedVisible = uiState.generatedRssFeed.isVisible,
      snackbarHostState = snackbarHostState,
      onDownloadClicked = {
        viewModel.onEvent(BookEvent.DownloadEvent(CommonDownloadEvent.Download))
      },
      onDeleteDownloadClicked = {
        viewModel.onEvent(BookEvent.DownloadEvent(CommonDownloadEvent.DeleteDownload))
      },
      onPlayClicked = { playerController.onEvent(PlayerEvent.PlayBook(viewModel.id)) },
      onGeneratedRssFeedClicked = {
        showRssFeedSheet = true
        scope.launch { sheetState.show() }
      },
      onEditClicked = { onEditClicked(viewModel.id) },
    )
  }
}

@Composable
fun BookScreenContent(
  id: String = Defaults.BOOK_ID,
  cover: String = Defaults.BOOK_COVER,
  title: String = Defaults.BOOK_TITLE,
  author: String = Defaults.BOOK_AUTHOR,
  description: String = "",
  subtitle: String = "",
  duration: String = Defaults.BOOK_DURATION,
  remaining: String = Defaults.BOOK_REMAINING,
  narrator: String = Defaults.BOOK_NARRATOR,
  publishYear: String = Defaults.BOOK_PUBLISH_YEAR,
  publisher: String = Defaults.BOOK_PUBLISHER,
  genres: String = Defaults.BOOK_GENRES,
  language: String = Defaults.BOOK_LANGUAGE,
  progress: Int = Defaults.PROGRESS_PERCENT,
  canEdit: Boolean = false,
  isEbook: Boolean = false,
  downloadState: DownloadState = DownloadState.Unknown,
  playPause: PlayPauseControlState = PlayPauseControlState(enabled = true),
  generatedRssFeedVisible: Boolean = false,
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
  onDownloadClicked: () -> Unit = {},
  onDeleteDownloadClicked: () -> Unit = {},
  onPlayClicked: () -> Unit,
  onGeneratedRssFeedClicked: () -> Unit = {},
  onEditClicked: () -> Unit = {},
) {
  LazyColumn(
    modifier =
      Modifier.mySharedBound(Animations.containerKey(id)).fillMaxSize().padding(horizontal = 16.dp),
    reverseLayout = true,
    verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Bottom),
  ) {
    item {
      Spacer(modifier = Modifier.height(16.dp))
      if (!isEbook) {
        PlayDownloadAndEdit(
          playPause = playPause,
          downloadState = downloadState,
          snackbarHostState = snackbarHostState,
          onDownloadClicked = onDownloadClicked,
          onDeleteDownloadClicked = onDeleteDownloadClicked,
          onPlayClicked = onPlayClicked,
          canEdit = canEdit,
          extraActionPainterResId = if (generatedRssFeedVisible) R.drawable.rss else null,
          extraActionContentDescriptionResId =
            if (generatedRssFeedVisible) R.string.generated_rss_feed else null,
          onExtraActionClicked = if (generatedRssFeedVisible) onGeneratedRssFeedClicked else null,
          onEditClicked = onEditClicked,
        )
      }
      ProgressRow(progress, remaining)
      ExpandShrinkText(text = description)
      BookDetail(duration, narrator, publishYear, publisher, genres, language)
      ItemDetail(id, cover, title, author, subtitle = subtitle)
    }
  }
}

@Composable
private fun BookDetail(
  duration: String,
  narrator: String,
  publishYear: String,
  publisher: String,
  genres: String,
  language: String,
) {
  Column(
    modifier = Modifier.padding(vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    TextLabelValue(stringResource(R.string.duration), duration)
    TextLabelValue(stringResource(R.string.narrator), narrator)
    TextLabelValue(stringResource(R.string.publish_year), publishYear)
    TextLabelValue(stringResource(R.string.publisher), publisher)
    TextLabelValue(stringResource(R.string.genre), genres)
    TextLabelValue(stringResource(R.string.language), language)
  }
}

@ShelfDroidPreview
@Composable
fun BookScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { BookScreenContent(onPlayClicked = {}) }
}

@ShelfDroidPreview
@Composable
fun BookScreenContentDynamicPreview() {
  AnimatedPreviewWrapper(dynamicColor = true) { BookScreenContent(onPlayClicked = {}) }
}

@ShelfDroidPreview
@Composable
fun BookScreenContentLoadingPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    BookScreenContent(
      playPause =
        PlayPauseControlState(
          enabled = true,
          showPlayIcon = false,
          showLoadingIndicator = true,
        ),
      onPlayClicked = {},
    )
  }
}
