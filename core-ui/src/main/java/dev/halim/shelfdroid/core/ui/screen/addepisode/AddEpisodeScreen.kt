package dev.halim.shelfdroid.core.ui.screen.addepisode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericState.Failure
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisode
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeDownloadState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeFilterState
import dev.halim.shelfdroid.core.data.screen.addepisode.AddEpisodeUiState
import dev.halim.shelfdroid.core.ui.Animations
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverWithTitle
import dev.halim.shelfdroid.core.ui.components.VisibilityCircular
import dev.halim.shelfdroid.core.ui.components.VisibilityUp
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.extensions.enable
import dev.halim.shelfdroid.core.ui.extensions.enableAlpha
import dev.halim.shelfdroid.core.ui.mySharedElement
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import kotlinx.coroutines.launch

@Composable
fun AddEpisodeScreen(
  viewModel: AddEpisodeViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
  onDownloadEpisodeSuccess: () -> Unit,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()

  LaunchedEffect(uiState.downloadEpisodeState) {
    when (val state = uiState.downloadEpisodeState) {
      is GenericState.Success -> {
        viewModel.onEvent(AddEpisodeEvent.ResetDownloadEpisodeState)
        onDownloadEpisodeSuccess()
      }
      is Failure -> {
        state.errorMessage?.let { scope.launch { snackbarHostState.showErrorSnackbar(it) } }
      }
      else -> Unit
    }
  }

  AddEpisodeScreenContent(itemId = viewModel.id, uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun AddEpisodeScreenContent(
  downloadState: GenericState = GenericState.Idle,
  itemId: String = "",
  uiState: AddEpisodeUiState = AddEpisodeUiState(),
  onEvent: (AddEpisodeEvent) -> Unit = {},
) {
  val selectedEpisodesCount by
    remember(uiState.episodes) {
      mutableIntStateOf(
        uiState.episodes.filter { it.state == AddEpisodeDownloadState.ToBeDownloaded }.size
      )
    }

  val filteredEpisodes by
    remember(uiState.episodes, uiState.filterState) {
      mutableStateOf(filter(uiState.episodes, uiState.filterState))
    }

  val lazyListState = rememberLazyListState()
  val fabVisible by remember { derivedStateOf { lazyListState.isScrollInProgress.not() } }

  Scaffold(
    floatingActionButton = { FilterFab(fabVisible, uiState.filterState, onEvent) },
    bottomBar = {
      DownloadButton(downloadState, selectedEpisodesCount) {
        onEvent(AddEpisodeEvent.DownloadEpisodes)
      }
    },
  ) { paddingValues ->
    LazyColumn(
      state = lazyListState,
      modifier = Modifier.padding(paddingValues).fillMaxSize().padding(horizontal = 16.dp),
      reverseLayout = true,
      verticalArrangement = Arrangement.Bottom,
    ) {
      item {
        CoverWithTitle(
          cover = uiState.cover,
          coverAnimationKey = Animations.coverKey(itemId),
          title = uiState.title,
          titleAnimationKey = Animations.titleKey(itemId, uiState.title),
          subtitle = uiState.author,
          subtitleAnimationKey = Animations.authorKey(itemId, uiState.author),
        )
        Spacer(modifier = Modifier.height(16.dp))
      }

      items(items = filteredEpisodes, key = { it.url }) { episode ->
        AddEpisodeItem(episode) { onEvent(AddEpisodeEvent.CheckEpisode(episode.url, it)) }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
      }
    }
  }
}

@Composable
private fun DownloadButton(state: GenericState, count: Int, onDownloadClick: () -> Unit) {
  val isZero = count == 0
  val text =
    if (isZero) stringResource(R.string.no_episodes_selected)
    else pluralStringResource(id = R.plurals.download_episode_count, count = count, count)
  Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
    Button(onClick = onDownloadClick, modifier = Modifier.weight(1f), enabled = count > 0) {
      VisibilityCircular(state == GenericState.Loading) {
        Icon(
          painter = painterResource(R.drawable.download),
          contentDescription = stringResource(R.string.download),
          modifier = Modifier.padding(end = 8.dp),
        )
      }
      Text(text)
    }
  }
}

@Composable
private fun FilterFab(
  fabVisible: Boolean,
  filterState: AddEpisodeFilterState,
  onEvent: (AddEpisodeEvent.FilterEvent) -> Unit,
) {
  var showFilterDialog by remember { mutableStateOf(false) }

  FilterDialog(
    showDialog = showFilterDialog,
    filterState = filterState,
    onConfirm = { showFilterDialog = false },
    onDismiss = { showFilterDialog = false },
    onEvent = onEvent,
  )

  VisibilityUp(fabVisible) {
    ExtendedFloatingActionButton(
      text = { Text(text = stringResource(R.string.filter)) },
      icon = {
        Icon(
          painter = painterResource(R.drawable.filter),
          contentDescription = stringResource(R.string.filter),
        )
      },
      onClick = { showFilterDialog = true },
    )
  }
}

@Composable
private fun AddEpisodeItem(episode: AddEpisode, onCheckedChange: (Boolean) -> Unit) {
  val checked =
    episode.state == AddEpisodeDownloadState.Downloaded ||
      episode.state == AddEpisodeDownloadState.ToBeDownloaded

  val enabled = episode.state != AddEpisodeDownloadState.Downloaded

  Row(verticalAlignment = Alignment.CenterVertically) {
    Checkbox(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    Column(modifier = Modifier.weight(1f)) {
      Text(
        modifier =
          Modifier.mySharedElement(Animations.Companion.Episode.titleKey(episode.episodeId)),
        text = episode.title,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurface.enable(enabled),
      )
      Text(
        episode.description,
        modifier = Modifier.alpha(enabled.enableAlpha()),
        style = MaterialTheme.typography.labelSmall,
        maxLines = 2,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@ShelfDroidPreview
@Composable
fun AddEpisodeScreenContentPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) { AddEpisodeScreenContent() }
}
