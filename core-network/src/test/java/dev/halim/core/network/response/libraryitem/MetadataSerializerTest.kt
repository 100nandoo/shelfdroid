package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataSerializerTest {

  private val json =
    Json {
      coerceInputValues = true
      explicitNulls = false
      ignoreUnknownKeys = true
    }

  @Test
  fun decodePodcastMetadata_whenFeedUrlPresent_usesPodcastDeserializer() {
    val metadata =
      json.decodeFromString<Metadata>(
        """
        {
          "title": "Podcast title",
          "author": "Host",
          "feedUrl": "https://example.com/feed.xml",
          "description": "desc",
          "genres": ["Technology"],
          "type": "serial"
        }
        """
      )

    assertTrue(metadata is PodcastMetadata)
    metadata as PodcastMetadata
    assertEquals("Host", metadata.author)
    assertEquals("https://example.com/feed.xml", metadata.feedUrl)
    assertEquals("serial", metadata.type)
  }

  @Test
  fun decodePodcastMetadata_whenItunesIdIsString_parsesNullableInt() {
    val metadata =
      json.decodeFromString<Metadata>(
        """
        {
          "title": "Podcast title",
          "feedUrl": "https://example.com/feed.xml",
          "itunesId": "785545036"
        }
        """
      )

    assertTrue(metadata is PodcastMetadata)
    metadata as PodcastMetadata
    assertEquals("785545036", metadata.itunesId)
  }

  @Test
  fun decodePodcast_whenCoverPathAndTagsMissing_usesDefaults() {
    val podcast =
      json.decodeFromString<Podcast>(
        """
        {
          "libraryItemId": "podcast-1",
          "metadata": {
            "title": "Podcast title",
            "feedUrl": "https://example.com/feed.xml"
          },
          "episodes": []
        }
        """
      )

    assertEquals(null, podcast.coverPath)
    assertTrue(podcast.tags.isEmpty())
  }

  @Test
  fun decodePodcast_whenNullableServerFieldsAreNull_usesSafeDefaults() {
    val podcast =
      json.decodeFromString<Podcast>(
        """
        {
          "libraryItemId": "podcast-1",
          "metadata": { "title": "Podcast title", "feedUrl": "https://example.com/feed.xml" },
          "autoDownloadSchedule": null,
          "lastEpisodeCheck": null
        }
        """
      )

    assertEquals("", podcast.autoDownloadSchedule)
    assertEquals(0L, podcast.lastEpisodeCheck)
  }

  @Test
  fun decodeAudioFile_whenSizeExceedsIntRange_preservesSize() {
    val audioFile =
      json.decodeFromString<AudioFile>(
        """
        {
          "metadata": {
            "filename": "large-audiobook.mp3",
            "size": 2182503238
          }
        }
        """
      )

    assertEquals(2182503238L, audioFile.metadata.size)
  }

  @Test
  fun decodeAudioFile_whenAudiobookshelfReturnsNullFileMetadata_usesSafeDefaults() {
    val audioFile =
      json.decodeFromString<AudioFile>(
        """
        {
          "index": null,
          "ino": null,
          "metadata": {
            "filename": null,
            "ext": null,
            "path": null,
            "relPath": null,
            "size": null,
            "mtimeMs": null,
            "ctimeMs": null,
            "birthtimeMs": null
          },
          "trackNumFromMeta": null,
          "discNumFromMeta": null,
          "error": null,
          "language": null,
          "embeddedCoverArt": null
        }
        """
      )

    assertEquals(0, audioFile.index)
    assertEquals("", audioFile.ino)
    assertEquals("", audioFile.metadata.filename)
    assertEquals(0L, audioFile.metadata.size)
    assertEquals(null, audioFile.trackNumFromMeta)
    assertEquals(null, audioFile.error)
    assertEquals(null, audioFile.language)
    assertEquals(null, audioFile.embeddedCoverArt)
  }
}
