package dev.halim.shelfdroid.media.library

import android.annotation.SuppressLint
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaConstants
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.PodcastEpisodeEntity
import dev.halim.shelfdroid.core.database.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@SuppressLint("UnsafeOptInUsageError")
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CarMediaItemMapperTest {
  private val context = RuntimeEnvironment.getApplication()
  private val mapper = CarMediaItemMapper(context)

  @Test
  fun rootTabsAndLibrariesAreBrowsable() {
    val root = mapper.folder(CarMediaId.Root)
    assertItem(root, "car:root", MediaMetadata.MEDIA_TYPE_FOLDER_MIXED, playable = false)
    assertEquals("ShelfDroid", root.mediaMetadata.title)
    assertEquals("android.resource", root.mediaMetadata.artworkUri?.scheme)

    val folders =
      mapOf(
        CarMediaId.ContinueListening to MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
        CarMediaId.Books to MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS,
        CarMediaId.Podcasts to MediaMetadata.MEDIA_TYPE_FOLDER_PODCASTS,
        CarMediaId.Downloads to MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
      )
    for ((folder, type) in folders) {
      assertItem(mapper.folder(folder), folder.value, type, playable = false)
    }

    val library = LibraryEntity("lib", "My Books", "[]", 1, 0, "audiobookshelf")
    val item = mapper.library(library)
    assertItem(
      item,
      "car:library:lib",
      MediaMetadata.MEDIA_TYPE_FOLDER_AUDIO_BOOKS,
      playable = false,
    )
    assertEquals("My Books", item.mediaMetadata.title)
  }

  @Test
  fun bookCarriesProgressAndLocalArtworkWithoutAudio() {
    val book = catalogItem("book", isBook = true)
    val progress = ProgressEntity("progress", book.id, null, "", .42, 100.0, 42.0, 0, 0)
    val item = mapper.libraryItem(book, progress)
    val metadata = item.mediaMetadata

    assertItem(item, "car:book:book", MediaMetadata.MEDIA_TYPE_AUDIO_BOOK, playable = true)
    assertEquals("A Title", metadata.title)
    assertEquals("An Author", metadata.artist)
    assertEquals("content", metadata.artworkUri?.scheme)
    assertEquals("${context.packageName}.car-artwork", metadata.artworkUri?.authority)
    assertEquals(listOf("book"), metadata.artworkUri?.pathSegments)
    assertEquals(
      MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED,
      metadata.extras?.getInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS),
    )
    assertEquals(.42, metadata.extras?.getDouble(MediaConstants.EXTRAS_KEY_COMPLETION_PERCENTAGE))
  }

  @Test
  fun episodesArePlayableAndUseTheirPodcastArtwork() {
    val podcast = catalogItem("podcast", isBook = false)
    val episode =
      PodcastEpisodeEntity(
        id = "episode",
        libraryItemId = podcast.id,
        episodeIndex = null,
        season = null,
        episode = null,
        episodeType = null,
        title = "An Episode",
        subtitle = null,
        description = null,
        enclosureUrl = null,
        enclosureType = null,
        enclosureLength = null,
        pubDate = null,
        audioFile = "{}",
        audioTrack = "{}",
        publishedAt = null,
        addedAt = 0,
        updatedAt = 0,
      )
    val progress = ProgressEntity("progress", podcast.id, episode.id, "", .2, 100.0, 20.0, 1, 0)
    val folder = mapper.libraryItem(podcast)
    val item = mapper.episode(podcast, episode, progress)

    assertItem(folder, "car:podcast:podcast", MediaMetadata.MEDIA_TYPE_PODCAST, playable = false)
    assertItem(
      item,
      "car:episode:podcast:episode",
      MediaMetadata.MEDIA_TYPE_PODCAST_EPISODE,
      playable = true,
    )
    assertEquals("An Episode", item.mediaMetadata.title)
    assertEquals("A Title", item.mediaMetadata.subtitle)
    assertEquals(folder.mediaMetadata.artworkUri, item.mediaMetadata.artworkUri)
    assertEquals(
      MediaConstants.EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED,
      item.mediaMetadata.extras?.getInt(MediaConstants.EXTRAS_KEY_COMPLETION_STATUS),
    )
  }

  @Test
  fun missingCoverUsesBundledArtwork() {
    val book = catalogItem("book", isBook = true).copy(cover = "")
    val item = mapper.libraryItem(book)
    assertEquals("android.resource", item.mediaMetadata.artworkUri?.scheme)
    assertEquals(listOf("drawable", "ic_notification"), item.mediaMetadata.artworkUri?.pathSegments)
  }

  private fun assertItem(item: MediaItem, id: String, mediaType: Int, playable: Boolean) {
    assertEquals(id, item.mediaId)
    assertEquals(!playable, item.mediaMetadata.isBrowsable)
    assertEquals(playable, item.mediaMetadata.isPlayable)
    assertEquals(mediaType, item.mediaMetadata.mediaType)
    assertNull(item.localConfiguration)
    LibraryResult.ofItem(item, null)
  }

  private fun catalogItem(id: String, isBook: Boolean) =
    LibraryItemEntity(
      id = id,
      libraryId = "lib",
      author = "An Author",
      authorFirstLast = "An Author",
      authorLastFirst = "Author, An",
      title = "A Title",
      description = "",
      cover = "https://example.invalid/api/items/$id/cover",
      updatedAt = 0,
      rssFeed = null,
      inoId = "",
      duration = 100.0,
      isBook = if (isBook) 1 else 0,
      addedAt = 0,
    )
}
