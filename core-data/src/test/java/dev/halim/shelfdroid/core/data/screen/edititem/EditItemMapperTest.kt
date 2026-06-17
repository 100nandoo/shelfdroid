package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Author
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.BookMetadata
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastMetadata
import dev.halim.core.network.response.libraryitem.Series
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditItemMapperTest {

  @Test
  fun mapMedia_mapsBookIntoBookEditState() {
    val mapped =
      EditItemMapper.mapMedia(
        LibraryItem(
          id = "book-1",
          mediaType = "book",
          media =
            Book(
              tags = listOf("fiction"),
              coverPath = null,
              metadata =
                BookMetadata(
                  title = "Dune",
                  subtitle = "Part One",
                  authors = listOf(Author(name = "Frank Herbert")),
                  narrators = listOf("Scott Brick"),
                  series = listOf(Series(name = "Dune", sequence = "1")),
                  genres = listOf("Sci-Fi"),
                  publishedYear = "1965",
                  publisher = "Chilton",
                  description = "Epic.",
                  isbn = "isbn-1",
                  asin = "asin-1",
                  language = "en",
                  explicit = true,
                ),
              chapters = listOf(BookChapter(id = 7, title = "Arrakis", start = 1.0, end = 2.0)),
            ),
        )
      )

    assertEquals(EditItemMediaKind.Book, mapped.mediaKind)
    assertEquals("Dune", mapped.details.title)
    assertEquals(listOf("Frank Herbert"), mapped.details.authors)
    assertEquals(listOf("fiction"), mapped.details.tags)
    assertEquals("1965", mapped.details.publishedYear)
    assertEquals(1, mapped.chapters.size)
    assertEquals(7, mapped.chapters.single().id)
  }

  @Test
  fun mapMedia_mapsPodcastIntoPodcastEditState() {
    val mapped =
      EditItemMapper.mapMedia(
        LibraryItem(
          id = "podcast-1",
          mediaType = "podcast",
          media =
            Podcast(
              tags = listOf("tech"),
              coverPath = null,
              metadata =
                PodcastMetadata(
                  title = "Core Intuition",
                  author = "Marco Arment",
                  description = "Apple talk.",
                  genres = listOf("Technology"),
                  feedUrl = "https://example.com/feed.xml",
                  releaseDate = "2026-06-10",
                  itunesId = 42,
                  language = "en",
                  explicit = true,
                  type = "serial",
                ),
            ),
        )
      )

    assertEquals(EditItemMediaKind.Podcast, mapped.mediaKind)
    assertEquals("Core Intuition", mapped.details.title)
    assertEquals("Marco Arment", mapped.details.podcastAuthor)
    assertEquals("https://example.com/feed.xml", mapped.details.rssFeedUrl)
    assertEquals("2026-06-10", mapped.details.releaseDate)
    assertEquals("42", mapped.details.itunesId)
    assertEquals("serial", mapped.details.podcastType)
    assertEquals(emptyList<ChapterRow>(), mapped.chapters)
  }

  @Test
  fun buildUpdateRequest_forBookOnlyIncludesChangedBookFields() {
    val request =
      EditItemMapper.buildUpdateRequest(
        mediaKind = EditItemMediaKind.Book,
        original =
          DetailsForm(
            title = "Old Title",
            authors = listOf("Old Author"),
            language = "en",
            tags = listOf("old"),
          ),
        current =
          DetailsForm(
            title = "New Title",
            authors = listOf("New Author"),
            language = "fr",
            tags = listOf("new"),
          ),
      )

    val metadata = requireNotNull(request.metadata)
    assertEquals("New Title", metadata.title)
    assertEquals(listOf("New Author"), metadata.authors?.map { it.name })
    assertEquals("fr", metadata.language)
    assertEquals(listOf("new"), request.tags)
    assertNull(metadata.author)
    assertNull(metadata.feedUrl)
    assertNull(metadata.type)
  }

  @Test
  fun buildUpdateRequest_forPodcastOnlyIncludesChangedPodcastFields() {
    val request =
      EditItemMapper.buildUpdateRequest(
        mediaKind = EditItemMediaKind.Podcast,
        original =
          DetailsForm(
            title = "Old Show",
            genres = listOf("Tech"),
            tags = listOf("old"),
            description = "Old description",
            language = "en",
            explicit = false,
            podcastAuthor = "Old Host",
            rssFeedUrl = "https://old.example/feed.xml",
            releaseDate = "2026-06-01",
            itunesId = "12",
            podcastType = "episodic",
          ),
        current =
          DetailsForm(
            title = "New Show",
            genres = listOf("Business"),
            tags = listOf("new"),
            description = "New description",
            language = "fr",
            explicit = true,
            podcastAuthor = "New Host",
            rssFeedUrl = "https://new.example/feed.xml",
            releaseDate = "2026-06-17",
            itunesId = "34",
            podcastType = "serial",
          ),
      )

    val metadata = requireNotNull(request.metadata)
    assertEquals("New Show", metadata.title)
    assertEquals("New Host", metadata.author)
    assertEquals(listOf("Business"), metadata.genres)
    assertEquals("https://new.example/feed.xml", metadata.feedUrl)
    assertEquals("2026-06-17", metadata.releaseDate)
    assertEquals(34, metadata.itunesId)
    assertEquals("serial", metadata.type)
    assertEquals("fr", metadata.language)
    assertEquals(true, metadata.explicit)
    assertEquals(listOf("new"), request.tags)
    assertNull(metadata.authors)
    assertNull(metadata.subtitle)
    assertNull(metadata.series)
  }
}
