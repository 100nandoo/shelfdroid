package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.core.network.response.RssFeed
import dev.halim.core.network.response.RssFeedEpisode
import dev.halim.core.network.response.RssFeedMeta
import dev.halim.core.network.response.RssFeedsResponse
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.junit.Assert.assertEquals
import org.junit.Test

class RssFeedsMapperTest {

  @Test
  fun map_sortsFeedsByUpdatedAtDescending() {
    val result =
      RssFeedsMapper.map(
        response =
          RssFeedsResponse(
            feeds =
              listOf(
                feed(id = "old", updatedAt = 100L),
                feed(id = "new", updatedAt = 300L),
                feed(id = "mid", updatedAt = 200L),
              )
          ),
        currentWebBaseUrl = "https://app.example.com",
        formatDateTime = { "date-$it" },
      )

    assertEquals(listOf("new", "mid", "old"), result.map { it.id })
  }

  @Test
  fun resolvePublicFeedUrl_prefersServerAddressAndPreservesPathPrefix() {
    val url =
      RssFeedsMapper.resolvePublicFeedUrl(
        serverAddress = "https://media.example.com/audiobookshelf",
        currentWebBaseUrl = "https://fallback.example.com/root",
        feedUrl = "/feed/show-slug",
      )

    assertEquals("https://media.example.com/audiobookshelf/feed/show-slug", url)
  }

  @Test
  fun resolveCoverUrl_usesConfiguredBaseUrl() {
    val url =
      RssFeedsMapper.resolveCoverUrl(
        currentWebBaseUrl = "https://app.example.com/shelf",
        feedUrl = "/feed/show-slug",
      )

    assertEquals("https://app.example.com/shelf/feed/show-slug/cover", url)
  }

  @Test
  fun map_sortsEpisodesNewestFirstAndKeepsUndatedLast() {
    val newestPubDate = rfc1123DateTime(year = 2026, month = 7, day = 9, hour = 10, minute = 0)
    val oldestPubDate = rfc1123DateTime(year = 2026, month = 7, day = 7, hour = 9, minute = 30)

    val result =
      RssFeedsMapper.map(
        response =
          RssFeedsResponse(
            feeds =
              listOf(
                feed(
                  id = "feed-1",
                  episodes =
                    listOf(
                      episode(id = "undated-1", title = "Undated 1", pubDate = null),
                      episode(id = "old", title = "Old", pubDate = oldestPubDate),
                      episode(id = "new", title = "New", pubDate = newestPubDate),
                      episode(id = "undated-2", title = "Undated 2", pubDate = null),
                    ),
                )
              )
          ),
        currentWebBaseUrl = "https://app.example.com",
        formatDateTime = { "date-$it" },
      )

    assertEquals(
      listOf("new", "old", "undated-1", "undated-2"),
      result.single().episodes.map { it.id },
    )
  }

  private fun feed(
    id: String,
    updatedAt: Long = 0L,
    episodes: List<RssFeedEpisode> = emptyList(),
  ): RssFeed =
    RssFeed(
      id = id,
      slug = "$id-slug",
      entityType = "libraryItem",
      feedUrl = "/feed/$id",
      updatedAt = updatedAt,
      meta = RssFeedMeta(title = "Feed $id"),
      episodes = episodes,
    )

  private fun episode(id: String, title: String, pubDate: String?): RssFeedEpisode =
    RssFeedEpisode(id = id, title = title, pubDate = pubDate)

  private fun rfc1123DateTime(
    year: Int,
    month: Int,
    day: Int,
    hour: Int,
    minute: Int,
  ): String =
    ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC)
      .format(DateTimeFormatter.RFC_1123_DATE_TIME)
}
