package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.Book
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryItemRepoTest {

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
}
