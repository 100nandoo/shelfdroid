package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.SearchBookMatchResponse
import dev.halim.core.network.response.SearchPodcast
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
          draft =
            PodcastMatchDraft(
              title = "Edited title",
              author = "Edited author",
              feedUrl = "https://edited.example/feed.xml",
              itunesId = "123",
              releaseDate = "2026-06-19",
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

    assertEquals("Edited title", updated.details.title)
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
        result =
          PodcastMatchResultRow(cover = "", title = "New title", author = "", explicit = true),
        draft = PodcastMatchDraft(title = "Edited title", explicit = true),
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
  fun buildPodcastMatchReviewUpdateRequest_onlyPersistsSelectedFields() {
    val review =
      PodcastMatchReviewState(
        result =
          PodcastMatchResultRow(
            cover = "https://example.com/new.jpg",
            title = "Matched title",
            author = "Matched author",
            genres = listOf("Technology"),
            feedUrl = "https://example.com/new.xml",
            itunesId = "77",
            releaseDate = "2026-06-19",
            explicit = true,
          ),
        draft =
          PodcastMatchDraft(
            title = "Edited title",
            author = "Edited author",
            feedUrl = "https://example.com/edited.xml",
            itunesId = "88",
            releaseDate = "2026-06-20",
            explicit = true,
          ),
        selectedFields =
          setOf(
            PodcastMatchField.Cover,
            PodcastMatchField.Title,
            PodcastMatchField.RssFeedUrl,
          ),
      )
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        details =
          DetailsForm(
            title = "Unsaved local title",
            podcastAuthor = "Unsaved local author",
            language = "fr",
          ),
        originalDetails =
          DetailsForm(
            title = "Original title",
            podcastAuthor = "Original author",
            rssFeedUrl = "https://example.com/original.xml",
            language = "en",
          ),
        match = MatchState.Podcast(review = review),
      )

    val request = buildPodcastMatchReviewUpdateRequest(state, review)
    val metadata = requireNotNull(request.metadata)

    assertEquals("Edited title", metadata.title)
    assertEquals("https://example.com/edited.xml", metadata.feedUrl)
    assertEquals("https://example.com/new.jpg", request.url)
    assertNull(metadata.author)
    assertNull(metadata.genres)
    assertNull(metadata.language)
  }

  @Test
  fun buildPodcastMatchReviewUpdateRequest_sendsCheckedFieldsEvenWhenTheyMatchPersistedState() {
    val review =
      PodcastMatchReviewState(
        result =
          PodcastMatchResultRow(
            cover = "",
            title = "Original title",
            author = "",
            explicit = false,
          ),
        draft = PodcastMatchDraft(title = "Original title", explicit = false),
        selectedFields = setOf(PodcastMatchField.Title),
      )
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Podcast,
        details = DetailsForm(title = "Unsaved local title"),
        originalDetails = DetailsForm(title = "Original title"),
        match = MatchState.Podcast(review = review),
      )

    val request = buildPodcastMatchReviewUpdateRequest(state, review)
    val metadata = requireNotNull(request.metadata)

    assertEquals("Original title", metadata.title)
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

  @Test
  fun applyBookReviewToState_onlyTouchesSelectedFieldsAndStagesCoverLocally() {
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Book,
        currentTab = EditItemTab.Match,
        coverUrl = "https://example.com/original.jpg",
        details =
          DetailsForm(
            title = "Old title",
            subtitle = "Old subtitle",
            authors = listOf("Old author"),
            narrators = listOf("Old narrator"),
            series = listOf(SeriesEntry("Old series", "1")),
            genres = listOf("Old genre"),
            tags = listOf("old-tag"),
            publisher = "Old publisher",
            publishedYear = "1999",
            description = "Old description",
            isbn = "old-isbn",
            asin = "old-asin",
            abridged = true,
          ),
      )

    val updated =
      applyBookReviewToState(
        state,
        BookMatchReviewState(
          result =
            BookMatchReviewResult(
              cover = "https://example.com/new.jpg",
              title = "Matched title",
              authors = listOf("Matched author"),
            ),
          draft =
            BookMatchDraft(
              title = "Edited title",
              subtitle = "Edited subtitle",
              authors = listOf("Edited author"),
              narrators = listOf("Edited narrator"),
              series = listOf(SeriesEntry("Edited series", "2")),
              genres = listOf("Edited genre"),
              tags = listOf("edited-tag"),
              publisher = "Edited publisher",
              publishedYear = "2026",
              description = "Edited description",
              isbn = "edited-isbn",
              asin = "edited-asin",
              abridged = false,
            ),
          selectedFields =
            setOf(
              BookMatchField.Cover,
              BookMatchField.Title,
              BookMatchField.Authors,
              BookMatchField.Abridged,
              BookMatchField.Genres,
            ),
        ),
      )

    assertEquals("Edited title", updated.details.title)
    assertEquals("Old subtitle", updated.details.subtitle)
    assertEquals(listOf("Edited author"), updated.details.authors)
    assertEquals(listOf("Old narrator"), updated.details.narrators)
    assertEquals(listOf("Old genre"), state.details.genres)
    assertEquals(listOf("Edited genre"), updated.details.genres)
    assertEquals(listOf("old-tag"), updated.details.tags)
    assertEquals("Old publisher", updated.details.publisher)
    assertFalse(updated.details.abridged)
    assertEquals("https://example.com/new.jpg", updated.pendingCoverUrl)
  }

  @Test
  fun buildBookMatchReviewUpdateRequest_onlyPersistsSelectedFields() {
    val review =
      BookMatchReviewState(
        result =
          BookMatchReviewResult(
            cover = "https://example.com/new.jpg",
            title = "Matched title",
            subtitle = "Matched subtitle",
            authors = listOf("Matched author"),
            narrators = listOf("Matched narrator"),
            publisher = "Matched publisher",
            publishedYear = "2026",
            description = "Matched description",
            isbn = "matched-isbn",
            asin = "matched-asin",
            abridged = false,
            genres = listOf("Technology"),
            tags = listOf("featured"),
            series = listOf(SeriesEntry("Matched series", "3")),
          ),
        draft =
          BookMatchDraft(
            title = "Edited title",
            subtitle = "Edited subtitle",
            authors = listOf("Edited author"),
            narrators = listOf("Edited narrator"),
            publisher = "Edited publisher",
            publishedYear = "2027",
            description = "Edited description",
            isbn = "edited-isbn",
            asin = "edited-asin",
            abridged = false,
            genres = listOf("Edited genre"),
            tags = listOf("edited-tag"),
            series = listOf(SeriesEntry("Edited series", "4")),
          ),
        selectedFields =
          setOf(
            BookMatchField.Cover,
            BookMatchField.Title,
            BookMatchField.Authors,
            BookMatchField.Tags,
            BookMatchField.Series,
          ),
      )
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Book,
        details =
          DetailsForm(
            title = "Unsaved local title",
            authors = listOf("Unsaved local author"),
            tags = listOf("local-tag"),
          ),
        originalDetails =
          DetailsForm(
            title = "Original title",
            authors = listOf("Original author"),
            tags = listOf("original-tag"),
            publisher = "Original publisher",
          ),
        match = MatchState.Book(review = review),
      )

    val request = buildBookMatchReviewUpdateRequest(state, review)
    val metadata = requireNotNull(request.metadata)

    assertEquals("Edited title", metadata.title)
    assertEquals(listOf(UpdateLibraryItemMediaRequest.NameRef("Edited author")), metadata.authors)
    assertEquals(
      listOf(UpdateLibraryItemMediaRequest.SeriesRef("Edited series", "4")),
      metadata.series,
    )
    assertEquals(listOf("edited-tag"), request.tags)
    assertEquals("https://example.com/new.jpg", request.url)
    assertNull(metadata.subtitle)
    assertNull(metadata.publisher)
    assertNull(metadata.abridged)
    assertNull(metadata.genres)
  }

  @Test
  fun buildBookMatchReviewUpdateRequest_sendsCheckedAbridgedWhenFalse() {
    val review =
      BookMatchReviewState(
        result = BookMatchReviewResult(title = "Matched title", abridged = false),
        draft = BookMatchDraft(title = "Matched title", abridged = false),
        selectedFields = setOf(BookMatchField.Abridged),
      )
    val state =
      EditItemUiState(
        mediaKind = EditItemMediaKind.Book,
        details = DetailsForm(title = "Unsaved local title", abridged = true),
        originalDetails = DetailsForm(title = "Original title", abridged = true),
        match = MatchState.Book(review = review),
      )

    val request = buildBookMatchReviewUpdateRequest(state, review)
    val metadata = requireNotNull(request.metadata)

    assertEquals(false, metadata.abridged)
  }

  @Test
  fun defaultBookMatchFields_excludesMissingOptionalFields() {
    val fields =
      defaultBookMatchFields(
        BookMatchReviewResult(
          title = "Title",
          authors = emptyList(),
          narrators = emptyList(),
          abridged = null,
          genres = emptyList(),
          tags = emptyList(),
          series = emptyList(),
        )
      )

    assertTrue(BookMatchField.Title in fields)
    assertFalse(BookMatchField.Cover in fields)
    assertFalse(BookMatchField.Authors in fields)
    assertFalse(BookMatchField.Narrators in fields)
    assertFalse(BookMatchField.Abridged in fields)
    assertFalse(BookMatchField.Genres in fields)
    assertFalse(BookMatchField.Series in fields)
  }
}
