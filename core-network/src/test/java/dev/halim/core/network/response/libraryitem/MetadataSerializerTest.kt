package dev.halim.core.network.response.libraryitem

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MetadataSerializerTest {

  private val json = Json { ignoreUnknownKeys = true }

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
}
