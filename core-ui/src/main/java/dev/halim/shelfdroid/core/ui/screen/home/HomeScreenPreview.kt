@file:OptIn(ExperimentalSharedTransitionApi::class)

package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.data.screen.home.BookUiState
import dev.halim.shelfdroid.core.data.screen.home.HomeState
import dev.halim.shelfdroid.core.data.screen.home.HomeUiState
import dev.halim.shelfdroid.core.data.screen.home.LibraryUiState
import dev.halim.shelfdroid.core.data.screen.home.PodcastUiState
import dev.halim.shelfdroid.core.ui.preview.Defaults
import dev.halim.shelfdroid.core.ui.preview.PreviewWrapper
import dev.halim.shelfdroid.core.ui.preview.ShelfDroidPreview

private val previewHomeUiState =
  HomeUiState(
    homeState = HomeState.Success,
    librariesUiState =
      listOf(
        LibraryUiState(id = "1", name = "My Books"),
        LibraryUiState(id = "2", name = "My Podcasts"),
      ),
    libraryItemsUiState =
      mapOf(
        0 to
          listOf(
            BookUiState(
              id = Defaults.BOOK_ID,
              author = Defaults.BOOK_AUTHOR,
              title = Defaults.BOOK_TITLE,
              cover = Defaults.BOOK_COVER,
              url = "",
              progress = 0.3f,
            ),
            BookUiState(
              id = "book2",
              author = "George R. R. Martin",
              title = "A Game of Thrones",
              cover = Defaults.BOOK_COVER,
              url = "",
              progress = 0.7f,
            ),
          ),
        1 to
          listOf(
            PodcastUiState(
              id = "podcast1",
              author = Defaults.AUTHOR_NAME,
              title = Defaults.TITLE,
              cover = Defaults.IMAGE_URL,
              url = "",
              startTime = 0L,
              endTime = 3600000L,
              episodeCount = Defaults.EPISODES.size,
            )
          ),
      ),
  )

@ShelfDroidPreview
@Composable
fun PodcastScreenContentPreview() {
  PreviewWrapper(dynamicColor = false) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        HomeScreenContent(uiState = previewHomeUiState)
      }
    }
  }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) {
    SharedTransitionLayout {
      AnimatedContent(targetState = Unit) { animatedContentScope ->
        HomeScreenContent(uiState = previewHomeUiState)
      }
    }
  }
}
