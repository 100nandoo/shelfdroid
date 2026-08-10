package dev.halim.shelfdroid.core.data.catalog

import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.Book
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryItemRepositoryTest {

  @Test
  fun primaryInoId_returnsFirstAudioFileIno() {
    val book = Book(audioFiles = listOf(AudioFile(ino = "ino-1"), AudioFile(ino = "ino-2")))

    assertEquals("ino-1", book.primaryInoId())
  }

  @Test
  fun primaryInoId_returnsEmptyStringWhenAudioFilesMissing() {
    val book = Book(audioFiles = emptyList())

    assertEquals("", book.primaryInoId())
  }

  @Test
  fun stalePodcastEpisodeIds_returnsRemovedEpisodeIdsFromIncomingItems() {
    val items =
      listOf(
        LibraryItem(
          id = "podcast-1",
          mediaType = "podcast",
          media = Podcast(episodes = listOf(PodcastEpisode(id = "episode-2"))),
        ),
        LibraryItem(
          id = "book-1",
          mediaType = "book",
          media = Book(),
        ),
      )

    val staleIds =
      stalePodcastEpisodeIds(
        existingEpisodeIds = setOf("episode-1", "episode-2", "episode-3"),
        items = items,
      )

    assertEquals(setOf("episode-1", "episode-3"), staleIds.toSet())
  }
}
