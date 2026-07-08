@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screen.rssfeeds

import android.content.ClipData
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsApiState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.RssFeedsUiState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.MyAlertDialog
import dev.halim.shelfdroid.core.ui.components.MyTonalIconButton
import dev.halim.shelfdroid.core.ui.components.TextLabelSmall
import dev.halim.shelfdroid.core.ui.components.TextTitleSmall
import dev.halim.shelfdroid.core.ui.components.VisibilityDown
import dev.halim.shelfdroid.core.ui.components.showErrorSnackbar
import dev.halim.shelfdroid.core.ui.components.showSuccessSnackbar
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults.RSS_FEEDS_UI_STATE
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.screen.GenericMessageScreen
import kotlinx.coroutines.launch

@Composable
fun RssFeedsScreen(
  viewModel: RssFeedsViewModel = hiltViewModel(),
  snackbarHostState: SnackbarHostState,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val scope = rememberCoroutineScope()
  val clipboardManager = LocalClipboard.current
  val copiedMessage = stringResource(R.string.rss_url_copied)
  val closeFailedMessage = stringResource(R.string.failed_to_close_rss_feed)
  val closeSuccessMessage = stringResource(R.string.rss_feed_closed)
  val publicFeedLabel = stringResource(R.string.public_feed_url)

  var selectedFeedId by rememberSaveable { mutableStateOf<String?>(null) }
  var pendingCloseFeedId by rememberSaveable { mutableStateOf<String?>(null) }
  var showCloseDialog by rememberSaveable { mutableStateOf(false) }
  val selectedFeed =
    remember(uiState.feeds, selectedFeedId) { uiState.feeds.find { it.id == selectedFeedId } }
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  LaunchedEffect(selectedFeedId, selectedFeed) {
    if (selectedFeedId != null && selectedFeed == null) {
      showCloseDialog = false
      if (sheetState.isVisible) sheetState.hide()
      selectedFeedId = null
    }
  }

  LaunchedEffect(uiState.apiState) {
    when (val state = uiState.apiState) {
      is RssFeedsApiState.CloseFailure -> {
        scope.launch { snackbarHostState.showErrorSnackbar(state.message ?: closeFailedMessage) }
      }

      RssFeedsApiState.CloseSuccess -> {
        scope.launch { snackbarHostState.showSuccessSnackbar(closeSuccessMessage) }
      }

      else -> Unit
    }
  }

  MyAlertDialog(
    title = stringResource(R.string.close_rss_feed),
    text = stringResource(R.string.close_rss_feed_confirm),
    showDialog = showCloseDialog,
    confirmText = stringResource(R.string.close_rss_feed),
    dismissText = stringResource(R.string.cancel),
    onConfirm = {
      pendingCloseFeedId?.let { viewModel.onEvent(RssFeedsEvent.CloseFeed(it)) }
      pendingCloseFeedId = null
      showCloseDialog = false
    },
    onDismiss = {
      pendingCloseFeedId = null
      showCloseDialog = false
    },
  )

  if (selectedFeed != null) {
    RssFeedSheet(
      sheetState = sheetState,
      feed = selectedFeed,
      onDismiss = {
        scope.launch { sheetState.hide() }.invokeOnCompletion { selectedFeedId = null }
      },
      onCopyUrl = { value ->
        scope.launch {
          clipboardManager.setClipEntry(ClipData.newPlainText(publicFeedLabel, value).toClipEntry())
          snackbarHostState.showSuccessSnackbar(copiedMessage)
        }
      },
    )
  }

  RssFeedsContent(
    uiState = uiState,
    onFeedClicked = { feed ->
      selectedFeedId = feed.id
      scope.launch { sheetState.show() }
    },
    onCloseClicked = { feed ->
      pendingCloseFeedId = feed.id
      showCloseDialog = true
    },
  )
}

@Composable
private fun RssFeedsContent(
  uiState: RssFeedsUiState = RssFeedsUiState(),
  onFeedClicked: (RssFeedsUiState.RssFeedUi) -> Unit = {},
  onCloseClicked: (RssFeedsUiState.RssFeedUi) -> Unit = {},
) {
  Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
    VisibilityDown(
      uiState.state is GenericState.Loading || uiState.apiState is RssFeedsApiState.Loading
    ) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }

    val state = uiState.state
    if (state is GenericState.Failure) {
      GenericMessageScreen(state.errorMessage ?: "")
    }

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.Bottom,
      reverseLayout = true,
    ) {
      if (uiState.feeds.isEmpty() && uiState.state is GenericState.Success) {
        item(key = "empty") {
          GenericMessageScreen(
            stringResource(R.string.empty_type, stringResource(R.string.rss_feeds))
          )
        }
      }

      items(uiState.feeds, key = { it.id }) { feed ->
        RssFeedItem(
          feed = feed,
          onClick = { onFeedClicked(feed) },
          onCloseClick = { onCloseClicked(feed) },
        )
        HorizontalDivider()
      }
    }

    Spacer(modifier = Modifier.height(16.dp))
  }
}

@Composable
private fun RssFeedItem(
  feed: RssFeedsUiState.RssFeedUi,
  onClick: () -> Unit,
  onCloseClick: () -> Unit,
) {
  Row(
    modifier =
      Modifier.fillMaxWidth()
        .clickable(onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    CoverNoAnimation(
      modifier = Modifier.size(64.dp),
      coverUrl = feed.coverUrl,
      shape = RoundedCornerShape(8.dp),
      showFallback = true,
    )
    Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
      TextTitleSmall(
        modifier = Modifier.fillMaxWidth(),
        text = feed.title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Row(modifier = Modifier.fillMaxWidth()) {
        TextLabelSmall(
          modifier = Modifier.weight(1f).padding(top = 4.dp),
          text =
            "${entityTypeLabel(feed.entityType)} ∙ ${feed.episodeCount} ${
              pluralStringResource(
                R.plurals.plurals_episode,
                feed.episodeCount,
              )
            }",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        MyTonalIconButton(
          painterResId = R.drawable.delete,
          contentDescriptionResId = R.string.close_rss_feed,
          onClick = onCloseClick,
        )
      }
    }
  }
}

@ShelfDroidPreview
@Composable
private fun RssFeedsScreenPreview() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    RssFeedsContent(uiState = RSS_FEEDS_UI_STATE)
  }
}
