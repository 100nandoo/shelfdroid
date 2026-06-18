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
}
