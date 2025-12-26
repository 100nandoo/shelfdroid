package dev.halim.shelfdroid.core.ui.screen.book

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.DownloadState
import dev.halim.shelfdroid.core.ExoState
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.InitMediaControllerIfMainActivity
import dev.halim.shelfdroid.core.ui.LocalAnimatedContentScope
import dev.halim.shelfdroid.core.ui.LocalSharedTransitionScope
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
import dev.halim.shelfdroid.core.ui.components.PlayAndDownload
import dev.halim.shelfdroid.core.ui.event.CommonDownloadEvent
import dev.halim.shelfdroid.core.ui.mySharedBound
import dev.halim.shelfdroid.core.ui.player.PlayerEvent
import dev.halim.shelfdroid.core.ui.player.PlayerViewModel
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.home.item.ItemDetail

@Composable
fun BookScreen(
  viewModel: BookViewModel = hiltViewModel(),
  playerViewModel: PlayerViewModel,
  snackbarHostState: SnackbarHostState,
) {
  InitMediaControllerIfMainActivity()
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val playerUiState by playerViewModel.uiState.collectAsStateWithLifecycle()
  val downloadState = if (uiState.isSingleTrack) uiState.download.state else uiState.downloads.state
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
      downloadState = downloadState,
      currentItemId = playerUiState.id,
      exoState = playerUiState.exoState,
      snackbarHostState = snackbarHostState,
      onDownloadClicked = {
        viewModel.onEvent(BookEvent.DownloadEvent(CommonDownloadEvent.Download))
      },
      onDeleteDownloadClicked = {
        viewModel.onEvent(BookEvent.DownloadEvent(CommonDownloadEvent.DeleteDownload))
      },
      onPlayClicked = {
        playerViewModel.onEvent(PlayerEvent.PlayBook(viewModel.id, downloadState.isDownloaded()))
      },
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
  progress: String = Defaults.PROGRESS_PERCENT,
  downloadState: DownloadState = DownloadState.Unknown,
  currentItemId: String = Defaults.BOOK_ID,
  exoState: ExoState = ExoState.Pause,
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
  onDownloadClicked: () -> Unit = {},
  onDeleteDownloadClicked: () -> Unit = {},
  onPlayClicked: () -> Unit,
) {
  val sharedTransitionScope = LocalSharedTransitionScope.current
  val animatedContentScope = LocalAnimatedContentScope.current

  val isPlaying = currentItemId == id && exoState == ExoState.Playing

  with(sharedTransitionScope) {
    with(animatedContentScope) {
      LazyColumn(
        modifier =
          Modifier.mySharedBound(Animations.containerKey(id))
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.Bottom),
      ) {
        item {
          Spacer(modifier = Modifier.height(16.dp))
          PlayAndDownload(
            isPlaying = isPlaying,
            downloadState = downloadState,
            snackbarHostState = snackbarHostState,
            onDownloadClicked = onDownloadClicked,
            onDeleteDownloadClicked = onDeleteDownloadClicked,
            onPlayClicked = onPlayClicked,
          )
          ProgressRow(progress, remaining)
          ExpandShrinkText(text = description)
          BookDetail(duration, narrator, publishYear, publisher, genres, language)
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ItemDetail(id, cover, title, author, subtitle = subtitle)
          }
        }
      }
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
    BookDetailRow(stringResource(R.string.duration), duration)
    BookDetailRow(stringResource(R.string.narrator), narrator)
    BookDetailRow(stringResource(R.string.publish_year), publishYear)
    BookDetailRow(stringResource(R.string.publisher), publisher)
    BookDetailRow(stringResource(R.string.genre), genres)
    BookDetailRow(stringResource(R.string.language), language)
  }
}

@Composable
private fun BookDetailRow(label: String, value: String) {
  if (value.isNotEmpty()) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(
        text = "$label: ",
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.weight(1f),
      )
      Text(
        text = value,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier.weight(4f),
      )
    }
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
