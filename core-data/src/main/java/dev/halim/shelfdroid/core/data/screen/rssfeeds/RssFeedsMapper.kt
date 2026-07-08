package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.core.network.response.RssFeed
import dev.halim.core.network.response.RssFeedEpisode
import dev.halim.core.network.response.RssFeedsResponse
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

internal object RssFeedsMapper {
  fun map(
    response: RssFeedsResponse,
    currentWebBaseUrl: String,
    formatDateTime: (Long) -> String,
  ): List<RssFeedsUiState.RssFeedUi> {
    return response.feeds
      .withIndex()
      .sortedWith(
        compareByDescending<IndexedValue<RssFeed>> { it.value.updatedAt }.thenBy { it.index }
      )
      .map { (_, feed) ->
        RssFeedsUiState.RssFeedUi(
          id = feed.id,
          title = feed.meta.title.ifBlank { feed.slug },
          slug = feed.slug,
          entityType = feed.entityType,
          episodeCount = feed.episodes.size,
          preventIndexing = feed.meta.preventIndexing,
          updatedAtText = formatDateTime(feed.updatedAt),
          publicFeedUrl = resolvePublicFeedUrl(feed.serverAddress, currentWebBaseUrl, feed.feedUrl),
          coverUrl = resolveCoverUrl(currentWebBaseUrl, feed.feedUrl),
          ownerName = feed.meta.ownerName?.takeIf { it.isNotBlank() },
          ownerEmail = feed.meta.ownerEmail?.takeIf { it.isNotBlank() },
          episodes = mapEpisodes(feed.episodes, formatDateTime),
        )
      }
  }

  private fun mapEpisodes(
    episodes: List<RssFeedEpisode>,
    formatDateTime: (Long) -> String,
  ): List<RssFeedsUiState.EpisodeUi> {
    return episodes
      .withIndex()
      .sortedWith(
        compareByDescending<IndexedValue<RssFeedEpisode>> {
            parsePubDateMillis(it.value.pubDate) ?: Long.MIN_VALUE
          }
          .thenBy { it.index }
      )
      .map { (_, episode) ->
        val publishedAtText =
          parsePubDateMillis(episode.pubDate)?.let(formatDateTime)
            ?: episode.pubDate?.takeIf { it.isNotBlank() }
        RssFeedsUiState.EpisodeUi(
          id = episode.id,
          title = episode.title,
          publishedAtText = publishedAtText,
        )
      }
  }

  internal fun resolvePublicFeedUrl(
    serverAddress: String?,
    currentWebBaseUrl: String,
    feedUrl: String,
  ): String {
    if (feedUrl.startsWith("http://") || feedUrl.startsWith("https://")) return feedUrl
    val baseUrl =
      AudiobookshelfBaseUrl.parse(serverAddress.orEmpty())
        ?: AudiobookshelfBaseUrl.parse(currentWebBaseUrl)
        ?: AudiobookshelfBaseUrl.DEFAULT
    return baseUrl.resolve(feedUrl)
  }

  internal fun resolveCoverUrl(currentWebBaseUrl: String, feedUrl: String): String {
    val coverPath = feedUrl.trimEnd('/') + "/cover"
    if (coverPath.startsWith("http://") || coverPath.startsWith("https://")) return coverPath
    val baseUrl = AudiobookshelfBaseUrl.parse(currentWebBaseUrl) ?: AudiobookshelfBaseUrl.DEFAULT
    return baseUrl.resolve(coverPath)
  }

  internal fun parsePubDateMillis(pubDate: String?): Long? {
    if (pubDate.isNullOrBlank()) return null
    return runCatching {
        ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
          .toInstant()
          .toEpochMilli()
      }
      .getOrNull()
  }
}
