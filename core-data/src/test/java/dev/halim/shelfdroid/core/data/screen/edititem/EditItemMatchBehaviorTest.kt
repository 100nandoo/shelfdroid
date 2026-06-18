package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.core.network.response.SearchPodcast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditItemMatchBehaviorTest {

  @Test
  fun mapPodcastMatchRows_preservesPodcastSpecificSummaryFields() {
    val rows =
      mapPodcastMatchRows(
        listOf(
          SearchPodcast(
            id = 42,
            artistId = 7,
            title = "Android Show",
            artistName = "Google",
            description = "Rich description",
            descriptionPlain = "Plain description",
            releaseDate = "2026-06-17",
            genres = listOf("Technology", "Software"),
            cover = "https://example.com/cover.jpg",
            trackCount = 88,
            feedUrl = "https://example.com/feed.xml",
            pageUrl = "https://example.com/podcast",
            explicit = true,
          )
        )
      )

    val row = rows.single()
    assertEquals("Android Show", row.title)
    assertEquals("Google", row.author)
    assertEquals(listOf("Technology", "Software"), row.genres)
    assertEquals(88, row.episodeCount)
    assertEquals("https://example.com/feed.xml", row.feedUrl)
    assertEquals("42", row.itunesId)
    assertEquals("2026-06-17", row.releaseDate)
    assertEquals("Plain description", row.description)
    assertTrue(row.explicit)
  }

  @Test
  fun applyPodcastReviewToState_onlyTouchesSelectedFieldsAndStagesCoverLocally() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        currentTab = EditItemTab.Match,
        coverUrl = "https://example.com/original.jpg",
        details =
          DetailsForm(
            title = "Old title",
            podcastAuthor = "Old author",
            genres = listOf("Old genre"),
            rssFeedUrl = "https://old.example/feed.xml",
            releaseDate = "2026-01-01",
            itunesId = "1",
            explicit = false,
          ),
      )

    val updated =
      applyPodcastReviewToState(
        state,
        PodcastMatchReviewState(
          result =
            PodcastMatchResultRow(
              cover = "https://example.com/new.jpg",
              title = "New title",
              author = "New author",
              genres = listOf("Tech"),
              feedUrl = "https://new.example/feed.xml",
              itunesId = "99",
              releaseDate = "2026-06-17",
              explicit = true,
            ),
          selectedFields =
            setOf(
              PodcastMatchField.Cover,
              PodcastMatchField.Title,
              PodcastMatchField.Genres,
              PodcastMatchField.Explicit,
            ),
        ),
      )

    assertEquals("New title", updated.details.title)
    assertEquals("Old author", updated.details.podcastAuthor)
    assertEquals(listOf("Tech"), updated.details.genres)
    assertEquals("https://old.example/feed.xml", updated.details.rssFeedUrl)
    assertEquals("1", updated.details.itunesId)
    assertEquals("2026-01-01", updated.details.releaseDate)
    assertTrue(updated.details.explicit)
    assertEquals("https://example.com/new.jpg", updated.pendingCoverUrl)
  }

  @Test
  fun applyPodcastMatchSelection_returnsToDetailsAndClearsReview() {
    val review =
      PodcastMatchReviewState(
        result = PodcastMatchResultRow(cover = "", title = "New title", author = "", explicit = true),
        selectedFields = setOf(PodcastMatchField.Title),
      )
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        currentTab = EditItemTab.Match,
        match = MatchState.Podcast(review = review),
      )

    val updated = applyPodcastMatchSelection(state, review)

    assertEquals(EditItemTab.Details, updated.currentTab)
    assertEquals(null, (updated.match as MatchState.Podcast).review)
  }

  @Test
  fun defaultPodcastMatchFields_excludesMissingOptionalFields() {
    val fields =
      defaultPodcastMatchFields(
        PodcastMatchResultRow(
          cover = "",
          title = "Title",
          author = "",
          genres = emptyList(),
          feedUrl = "",
          itunesId = "",
          releaseDate = "",
          explicit = false,
        )
      )

    assertTrue(PodcastMatchField.Title in fields)
    assertTrue(PodcastMatchField.Explicit in fields)
    assertFalse(PodcastMatchField.Cover in fields)
    assertFalse(PodcastMatchField.Author in fields)
  }

  @Test
  fun applyBookMatchResult_preservesExistingBehavior() {
    val updated =
      applyBookMatchResult(
        details =
          DetailsForm(
            title = "Old",
            authors = listOf("Old Author"),
            narrators = listOf("Old Narrator"),
            genres = listOf("Old Genre"),
          ),
        result =
          SearchBookMatchResponse(
            title = "New",
            author = "New Author, Co Author",
            narrator = "New Narrator",
            genres = listOf("Sci-Fi"),
          ),
      )

    assertEquals("New", updated.title)
    assertEquals(listOf("New Author", "Co Author"), updated.authors)
    assertEquals(listOf("New Narrator"), updated.narrators)
    assertEquals(listOf("Sci-Fi"), updated.genres)
  }
}
