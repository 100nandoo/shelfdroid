@file:OptIn(ExperimentalMaterial3Api::class)

package dev.halim.shelfdroid.core.ui.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.PlaybackProgress
import dev.halim.shelfdroid.core.SeekControlsState
import dev.halim.shelfdroid.core.ui.R
import dev.halim.shelfdroid.core.ui.components.CoverNoAnimation
import dev.halim.shelfdroid.core.ui.components.MyAlertDialogWithCheckbox
import dev.halim.shelfdroid.core.ui.player.SmallPlayerContent
import dev.halim.shelfdroid.core.ui.player.bigplayer.BigPlayerContent
import dev.halim.shelfdroid.core.ui.player.bigplayer.ChapterBottomSheet
import dev.halim.shelfdroid.core.ui.preview.AnimatedPreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview
import dev.halim.shelfdroid.core.ui.preview.sheetState
import dev.halim.shelfdroid.core.ui.screen.book.BookScreenContent
import dev.halim.shelfdroid.core.ui.screen.episode.EpisodeScreenContent
import dev.halim.shelfdroid.core.ui.screen.home.HomeScreenContent
import dev.halim.shelfdroid.core.ui.screen.login.LoginScreenContent
import dev.halim.shelfdroid.core.ui.screen.podcast.PodcastScreenContent

@PreviewTest
@ShelfDroidPreview
@Composable
fun HomeScreenGridScreenshot() {
  val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(pagerState = pagerState, uiState = Defaults.HOME_UI_STATE)
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun HomeScreenListScreenshot() {
  val pagerState = rememberPagerState(initialPage = 1, pageCount = { 2 })
  AnimatedPreviewWrapper(dynamicColor = false) {
    HomeScreenContent(pagerState = pagerState, uiState = Defaults.HOME_UI_STATE_LIST)
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun BookScreenScreenshot() {
  AnimatedPreviewWrapper(dynamicColor = false) { BookScreenContent(onPlayClicked = {}) }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun PodcastScreenScreenshot() {
  AnimatedPreviewWrapper(dynamicColor = false) { PodcastScreenContent() }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun EpisodeScreenScreenshot() {
  AnimatedPreviewWrapper(dynamicColor = false) { EpisodeScreenContent() }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun LoginScreenScreenshot() {
  PreviewWrapper(dynamicColor = false) { LoginScreenContent() }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun SmallPlayerScreenshot() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    Column {
      Box(modifier = Modifier.weight(1f))
      SmallPlayerContent(onClicked = {}, onSwipeUp = {}, onSwipeDown = {})
    }
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun BigPlayerScreenshot() {
  AnimatedPreviewWrapper(dynamicColor = false) {
    BigPlayerContent(
      id = Defaults.BOOK_ID,
      title = Defaults.BOOK_TITLE,
      author = Defaults.BOOK_AUTHOR,
      progress = PlaybackProgress(position = 740, duration = 3_600, progress = 0.205f),
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER_LIST[1],
      bookmarks = Defaults.DEFAULT_PLAYER_BOOKMARK_LIST,
      newBookmarkTime = Defaults.DEFAULT_PLAYER_BOOKMARK,
      playPause = PlayPauseControlState(enabled = true),
      seekControls =
        SeekControlsState(
          seekBackEnabled = true,
          seekForwardEnabled = true,
          seekSliderEnabled = true,
        ),
    )
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun CoverFallbackScreenshot() {
  PreviewWrapper(dynamicColor = false) {
    CoverNoAnimation(modifier = Modifier.padding(16.dp), coverUrl = "")
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun DeleteDialogScreenshot() {
  PreviewWrapper(dynamicColor = false) {
    MyAlertDialogWithCheckbox(
      showDialog = true,
      title = stringResource(R.string.delete),
      text = stringResource(R.string.dialog_delete_episode),
      confirmText = stringResource(R.string.delete),
      onConfirm = {},
      checkboxChecked = true,
      onCheckboxChange = { _ -> },
      checkboxText = stringResource(R.string.delete_from_file_system),
    )
  }
}

@PreviewTest
@ShelfDroidPreview
@Composable
fun ChapterBottomSheetScreenshot() {
  PreviewWrapper(dynamicColor = false) {
    ChapterBottomSheet(
      sheetState = sheetState(LocalDensity.current),
      chapters = Defaults.DEFAULT_PLAYER_CHAPTER_LIST,
      currentChapter = Defaults.DEFAULT_PLAYER_CHAPTER,
    )
  }
}
