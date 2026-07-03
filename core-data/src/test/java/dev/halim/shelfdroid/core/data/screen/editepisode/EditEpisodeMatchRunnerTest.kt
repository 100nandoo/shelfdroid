package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.request.UpdatePodcastEpisodeRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.SearchPodcastEpisodeMatch
import dev.halim.core.network.response.libraryitem.Enclosure
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditEpisodeMatchRunnerTest {

  @Test
  fun search_mapsEpisodeResultsFromCurrentPodcastFeed() = runTest {
    var capturedItemId: String? = null
    var capturedTitle: String? = null
    val runner =
      EditEpisodeMatchRunner(
        searchEpisodes = { itemId, title ->
          capturedItemId = itemId
          capturedTitle = title
          Result.success(
            listOf(
              SearchPodcastEpisodeMatch(
                episode =
                  dev.halim.core.network.response.Episode(
                    title = "Matched title",
                    subtitle = "Matched subtitle",
                    description = "<p>Matched description</p>",
                    descriptionPlain = "Matched description",
                    pubDate = "Fri, 18 Jun 2026 03:30:00+0800",
                    episodeType = "bonus",
                    season = "2",
                    episode = "19",
                    author = "Host",
                    duration = "01:00:00",
                    explicit = "no",
                    publishedAt = 1781724600000L,
                    enclosure =
                      dev.halim.core.network.response.Enclosure(
                        url = "https://example.com/rss-episode.mp3",
                        type = "audio/mpeg",
                        length = "12345",
                      ),
                  )
              )
            )
          )
        },
        updateEpisode = { _, _, _ -> error("Should not patch while searching") },
      )

    val result =
      runner.search(
        state(
          match =
            EpisodeMatchState(
              searchTerm = "Matched title",
              isSearching = true,
            )
        )
      )

    assertEquals("item-1", capturedItemId)
    assertEquals("Matched title", capturedTitle)
    assertEquals(false, result.state.match.isSearching)
    assertEquals(true, result.state.match.hasSearched)
    assertEquals(null, result.state.match.errorMessage)
    assertEquals(1, result.state.match.results.size)
    assertEquals("19", result.state.match.results.single().episode)
    assertEquals("Matched title", result.state.match.results.single().title)
    assertEquals("Matched subtitle", result.state.match.results.single().subtitle)
    assertEquals("https://example.com/rss-episode.mp3", result.state.match.results.single().enclosureUrl)
    assertEquals(emptyList<GenericUiEvent>(), result.events)
  }

  @Test
  fun search_whenRequestFails_exposesErrorState() = runTest {
    val runner =
      EditEpisodeMatchRunner(
        searchEpisodes = { _, _ -> Result.failure(IllegalStateException("Search failed")) },
        updateEpisode = { _, _, _ -> error("Should not patch while searching") },
      )

    val result =
      runner.search(
        state(
          match =
            EpisodeMatchState(
              searchTerm = "Bad query",
              isSearching = true,
            )
        )
      )

    assertEquals(false, result.state.match.isSearching)
    assertEquals(true, result.state.match.hasSearched)
    assertEquals("Search failed", result.state.match.errorMessage)
    assertEquals(emptyList<EpisodeMatchResultRow>(), result.state.match.results)
    assertEquals(listOf(GenericUiEvent.ShowErrorSnackbar("Search failed")), result.events)
  }

  @Test
  fun apply_whenUpdateSucceeds_patchesMatchedFieldsAndReturnsToDetails() = runTest {
    var capturedRequest: UpdatePodcastEpisodeRequest? = null
    val runner =
      EditEpisodeMatchRunner(
        searchEpisodes = { _, _ -> error("Should not search while applying") },
        updateEpisode = { itemId, episodeId, request ->
          capturedRequest = request
          Result.success(
            updatedItem(
              itemId = itemId,
              episodeId = episodeId,
              title = "Server matched title",
              subtitle = "Server matched subtitle",
              description = "<p>Server matched description</p>",
              enclosureUrl = "https://example.com/server.mp3",
              season = "7",
              episode = "33",
              episodeType = "trailer",
              pubDate = "Fri, 18 Jun 2026 03:30:00+0800",
              publishedAt = 1781724600000L,
            )
          )
        },
      )

    val result =
      runner.apply(
        state(
          currentTab = EditEpisodeTab.Match,
          details = EpisodeDetailsForm(title = "Unsaved title", subtitle = "Unsaved subtitle"),
          originalDetails = EpisodeDetailsForm(title = "Original title"),
          match =
            EpisodeMatchState(
              searchTerm = "Original title",
              results =
                listOf(
                  EpisodeMatchResultRow(
                    season = "7",
                    episode = "33",
                    episodeType = "trailer",
                    title = "Matched title",
                    subtitle = "Matched subtitle",
                    description = "<p>Matched description</p>",
                    enclosureUrl = "https://example.com/matched.mp3",
                    enclosureType = "audio/mpeg",
                    enclosureLength = "4321",
                    pubDate = "Fri, 18 Jun 2026 03:30:00+0800",
                    publishedAtMillis = 1781724600000L,
                  )
                ),
            ),
          isSaving = true,
        ),
        index = 0,
      )

    assertEquals("Matched title", capturedRequest?.title)
    assertEquals("Matched subtitle", capturedRequest?.subtitle)
    assertEquals("<p>Matched description</p>", capturedRequest?.description)
    assertEquals("7", capturedRequest?.season)
    assertEquals("33", capturedRequest?.episode)
    assertEquals("trailer", capturedRequest?.episodeType)
    assertEquals("https://example.com/matched.mp3", capturedRequest?.enclosure?.url)
    assertEquals("audio/mpeg", capturedRequest?.enclosure?.type)
    assertEquals("4321", capturedRequest?.enclosure?.length)
    assertEquals("Fri, 18 Jun 2026 03:30:00+0800", capturedRequest?.pubDate)
    assertEquals(1781724600000L, capturedRequest?.publishedAt)
    assertEquals(EditEpisodeTab.Details, result.state.currentTab)
    assertEquals("Server matched title", result.state.details.title)
    assertEquals("Server matched subtitle", result.state.details.subtitle)
    assertEquals("<p>Server matched description</p>", result.state.details.description)
    assertEquals("https://example.com/server.mp3", result.state.details.enclosureUrl)
    assertEquals(result.state.details, result.state.originalDetails)
    assertEquals("Server matched title", result.state.match.searchTerm)
    assertEquals(false, result.state.isSaving)
    assertEquals(listOf(GenericUiEvent.ShowSuccessSnackbar()), result.events)
  }

  private fun state(
    currentTab: EditEpisodeTab = EditEpisodeTab.Details,
    details: EpisodeDetailsForm = EpisodeDetailsForm(title = "Episode title"),
    originalDetails: EpisodeDetailsForm = details,
    match: EpisodeMatchState = EpisodeMatchState(searchTerm = details.title),
    isSaving: Boolean = false,
  ) = EditEpisodeUiState(
    state = GenericState.Success,
    itemId = "item-1",
    episodeId = "ep-1",
    podcastTitle = "Podcast",
    currentTab = currentTab,
    details = details,
    originalDetails = originalDetails,
    match = match,
    isSaving = isSaving,
  )

  private fun updatedItem(
    itemId: String,
    episodeId: String,
    title: String,
    subtitle: String,
    description: String,
    enclosureUrl: String,
    season: String,
    episode: String,
    episodeType: String,
    pubDate: String,
    publishedAt: Long,
  ) = LibraryItem(
    id = itemId,
    libraryId = "library-1",
    mediaType = "podcast",
    media =
      Podcast(
        libraryItemId = itemId,
        episodes =
          listOf(
            PodcastEpisode(
              id = episodeId,
              title = title,
              subtitle = subtitle,
              description = description,
              enclosure = Enclosure(url = enclosureUrl),
              season = season,
              episode = episode,
              episodeType = episodeType,
              pubDate = pubDate,
              publishedAt = publishedAt,
            )
          ),
      ),
  )
}
