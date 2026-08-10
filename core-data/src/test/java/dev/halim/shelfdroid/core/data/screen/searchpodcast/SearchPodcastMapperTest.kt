package dev.halim.shelfdroid.core.data.screen.searchpodcast

import dev.halim.core.network.response.SearchPodcast
import dev.halim.shelfdroid.core.data.catalog.ExistingPodcastSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPodcastMapperTest {

  private val mapper = SearchPodcastMapper()

  @Test
  fun map_marksExistingPodcastAsAdded_whenSummaryMatchesSourceFeed() {
    val response =
      listOf(
        SearchPodcast(
          id = 42,
          artistId = 7,
          title = "Studio Dispatch",
          artistName = "Mara Lee",
          description = "desc",
          descriptionPlain = "desc",
          releaseDate = "2026-06-10",
          genres = listOf("Technology", "News"),
          cover = "cover",
          trackCount = 24,
          feedUrl = "https://example.com/feed.xml",
          pageUrl = "https://example.com/show",
          explicit = true,
        )
      )
    val existingPodcastSummaries =
      listOf(
        ExistingPodcastSummary(
          id = "podcast-1",
          itunesId = "",
          title = "Another Title",
          artist = "Another Author",
          feedUrl = "https://example.com/feed.xml",
        )
      )

    val result = mapper.map(response, existingPodcastSummaries, libraryId = "library-1").single()

    assertTrue(result.isAdded)
    assertEquals("podcast-1", result.id)
    assertEquals("library-1", result.payload.libraryId)
    assertEquals("https://example.com/feed.xml", result.payload.feedUrl)
  }

  @Test
  fun map_leavesUnmatchedPodcastAvailableToAdd() {
    val response =
      listOf(
        SearchPodcast(
          id = 99,
          artistId = null,
          title = "Queue Science",
          artistName = "Ana Silva",
          description = "desc",
          descriptionPlain = "desc",
          releaseDate = "2026-07-04",
          genres = listOf("Technology"),
          cover = "cover",
          trackCount = 12,
          feedUrl = "https://example.com/queue.xml",
          pageUrl = "https://example.com/queue",
          explicit = false,
        )
      )

    val result = mapper.map(response, emptyList(), libraryId = "library-2").single()

    assertFalse(result.isAdded)
    assertEquals("", result.id)
    assertEquals(99, result.payload.itunesId)
    assertEquals("library-2", result.payload.libraryId)
  }
}
