package dev.halim.shelfdroid.core.ui.screen.home

import androidx.compose.runtime.Composable
import dev.halim.shelfdroid.core.data.home.BookUiState
import dev.halim.shelfdroid.core.data.home.HomeState
import dev.halim.shelfdroid.core.data.home.HomeUiState
import dev.halim.shelfdroid.core.data.home.LibraryUiState
import dev.halim.shelfdroid.core.data.home.PodcastUiState
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
  PreviewWrapper(dynamicColor = false) { HomeScreenContent(uiState = previewHomeUiState) }
}

@ShelfDroidPreview
@Composable
fun PodcastScreenContentDynamicPreview() {
  PreviewWrapper(dynamicColor = true) { HomeScreenContent(uiState = previewHomeUiState) }
}
