package dev.halim.shelfdroid.core.ui.screen.podcast

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericState.Failure
import dev.halim.shelfdroid.core.data.screen.podcast.PodcastUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.InitMediaControllerIfMainActivity
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.ExpandShrinkText
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

  LaunchedEffect(uiState.addEpisodeState) {
    when (val state = uiState.addEpisodeState) {
      is GenericState.Success -> {
        viewModel.onEvent(PodcastEvent.ResetAddEpisodeState)
        onFetchEpisodeSuccess(viewModel.id)
      }
      is Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showSnackbar(it) } }
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
    ),
  id: String = Defaults.BOOK_ID,
  snackbarHostState: SnackbarHostState = SnackbarHostState(),
  onEvent: (PodcastEvent) -> Unit = {},
  onEpisodeClicked: (String, String) -> Unit = { _, _ -> },
  onPlayClicked: (String, String, Boolean) -> Unit = { _, _, _ -> },
) {
  val isDownloaded = uiState.displayPrefs.filter.isDownloaded()
  val episodes =
    if (isDownloaded) uiState.episodes.filter { it.download.state.isDownloaded() }
    else uiState.episodes

  LazyColumn(
    modifier = Modifier.mySharedBound(Animations.containerKey(id)).fillMaxSize(),
    reverseLayout = true,
  ) {
    item { Spacer(modifier = Modifier.height(12.dp)) }
    items(episodes) { episode ->
      HorizontalDivider()
      EpisodeItem(id, episode, onEvent, onEpisodeClicked, onPlayClicked, snackbarHostState)
    }
    item {
      Header(uiState.canAddEpisode, uiState.addEpisodeState, onEvent)
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
}

@Composable
private fun Header(canAddEpisode: Boolean, state: GenericState, onEvent: (PodcastEvent) -> Unit) {

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
      VisibilityCircular(state == GenericState.Loading) {
        Icon(
          painter = painterResource(id = R.drawable.search),
          contentDescription = stringResource(R.string.search_podcast),
          modifier = Modifier.clickable { onEvent(PodcastEvent.AddEpisode) },
        )
      }
    }
  }
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
