package dev.halim.shelfdroid.core.data.screen.edititem

import org.junit.Assert.assertEquals
import org.junit.Test

class EditItemUiStateTest {

  @Test
  fun supportedTabs_forPodcast_includesMatchAndExcludesTools() {
    assertEquals(
      listOf(
        EditItemTab.Details,
        EditItemTab.Cover,
        EditItemTab.Episodes,
        EditItemTab.Files,
        EditItemTab.Match,
        EditItemTab.Schedule,
      ),
      EditItemMediaKind.Podcast.supportedTabs(),
    )
  }

  @Test
  fun normalized_forPodcastCoercesUnsupportedToolsTabBackToDetails() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        currentTab = EditItemTab.Tools,
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
  fun supportedTabs_forBook_excludesSchedule() {
    assertEquals(false, EditItemMediaKind.Book.supportedTabs().contains(EditItemTab.Schedule))
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

  @Test
  fun hasScheduleChanges_ignoresCronDifferencesWhileDisabled() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        schedule =
          PodcastScheduleForm(
            autoDownloadEpisodes = false,
            cronExpression = "15 23 * * *",
            maxEpisodesToKeepInput = "0",
            maxNewEpisodesToDownloadInput = "3",
          ),
        originalSchedule =
          PodcastScheduleForm(
            autoDownloadEpisodes = false,
            cronExpression = "0 1 * * 1",
            maxEpisodesToKeepInput = "0",
            maxNewEpisodesToDownloadInput = "3",
          ),
      )

    assertEquals(false, state.hasScheduleChanges())
  }
}
