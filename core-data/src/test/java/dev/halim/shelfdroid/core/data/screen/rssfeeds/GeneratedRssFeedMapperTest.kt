package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.core.network.response.RssFeed
import dev.halim.core.network.response.RssFeedMeta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedRssFeedMapperTest {

  @Test
  fun map_withoutFeedOnlyShowsActionToManagersWithAudio() {
    val managerResult =
      GeneratedRssFeedMapper.map(
        itemId = "item-1",
        feed = null,
        webBaseUrl = "https://app.example.com/shelf",
        canManage = true,
        hasAudioContent = true,
        hasEpisodesWithoutPubDate = false,
      )
    val memberResult =
      GeneratedRssFeedMapper.map(
        itemId = "item-1",
        feed = null,
        webBaseUrl = "https://app.example.com/shelf",
        canManage = false,
        hasAudioContent = true,
        hasEpisodesWithoutPubDate = false,
      )

    assertTrue(managerResult.isVisible)
    assertFalse(memberResult.isVisible)
    assertEquals("item-1", managerResult.defaultSlug)
  }

  @Test
  fun map_withExistingFeedKeepsActionVisibleAndResolvesPublicUrl() {
    val result =
      GeneratedRssFeedMapper.map(
        itemId = "item-1",
        feed =
          RssFeed(
            id = "feed-1",
            entityType = "libraryItem",
            entityId = "item-1",
            feedUrl = "/feed/show-slug",
            meta = RssFeedMeta(title = "Show"),
          ),
        webBaseUrl = "https://app.example.com/shelf",
        canManage = false,
        hasAudioContent = false,
        hasEpisodesWithoutPubDate = true,
      )

    assertTrue(result.isVisible)
    assertEquals("https://app.example.com/shelf/feed/show-slug", result.publicFeedUrl)
    assertTrue(result.hasEpisodesWithoutPubDate)
  }
}
