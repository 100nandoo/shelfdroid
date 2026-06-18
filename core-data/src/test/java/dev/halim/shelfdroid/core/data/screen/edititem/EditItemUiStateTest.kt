package dev.halim.shelfdroid.core.data.screen.edititem

import org.junit.Assert.assertEquals
import org.junit.Test

class EditItemUiStateTest {

  @Test
  fun supportedTabs_forPodcast_onlyIncludesApprovedTabs() {
    assertEquals(
      listOf(EditItemTab.Details, EditItemTab.Cover, EditItemTab.Episodes, EditItemTab.Files),
      EditItemMediaKind.Podcast.supportedTabs(),
    )
  }

  @Test
  fun normalized_forPodcastCoercesUnsupportedTabBackToDetails() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        currentTab = EditItemTab.Match,
      )

    assertEquals(EditItemTab.Details, state.normalized().currentTab)
  }

  @Test
  fun normalized_forBookKeepsExistingSupportedTab() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Book,
        currentTab = EditItemTab.Tools,
      )

    assertEquals(EditItemTab.Tools, state.normalized().currentTab)
  }

  @Test
  fun supportedTabs_forBook_excludesEpisodes() {
    assertEquals(false, EditItemMediaKind.Book.supportedTabs().contains(EditItemTab.Episodes))
  }

  @Test
  fun switchingTabs_preservesEpisodeUpdateInput() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        currentTab = EditItemTab.Episodes,
        episodeUpdate =
          EpisodeUpdateState(
            persistedCutoffMillis = 1L,
            selectedCutoffMillis = 1L,
            limitInput = "0",
          ),
      )

    val switched =
      state.copy(currentTab = EditItemTab.Details).copy(currentTab = EditItemTab.Episodes)

    assertEquals(state.episodeUpdate, switched.episodeUpdate)
  }
}
