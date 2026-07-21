package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.core.network.response.RssFeed

data class GeneratedRssFeedDetails(
  val slug: String,
  val preventIndexing: Boolean,
  val ownerName: String,
  val ownerEmail: String,
)

data class GeneratedRssFeedUiState(
  val currentFeed: CurrentFeedUi? = null,
  val publicFeedUrl: String = "",
  val defaultSlug: String = "",
  val webBaseUrl: String = "",
  val canManage: Boolean = false,
  val isVisible: Boolean = false,
  val hasEpisodesWithoutPubDate: Boolean = false,
) {
  data class CurrentFeedUi(
    val id: String,
    val preventIndexing: Boolean,
    val ownerName: String?,
    val ownerEmail: String?,
  )
}

internal object GeneratedRssFeedMapper {
  fun map(
    itemId: String,
    feed: RssFeed?,
    webBaseUrl: String,
    canManage: Boolean,
    hasAudioContent: Boolean,
    hasEpisodesWithoutPubDate: Boolean,
  ): GeneratedRssFeedUiState {
    return GeneratedRssFeedUiState(
      currentFeed =
        feed?.let {
          GeneratedRssFeedUiState.CurrentFeedUi(
            id = it.id,
            preventIndexing = it.meta.preventIndexing,
            ownerName = it.meta.ownerName?.takeIf(String::isNotBlank),
            ownerEmail = it.meta.ownerEmail?.takeIf(String::isNotBlank),
          )
        },
      publicFeedUrl =
        feed
          ?.let { RssFeedsMapper.resolvePublicFeedUrl(it.serverAddress, webBaseUrl, it.feedUrl) }
          .orEmpty(),
      defaultSlug = itemId,
      webBaseUrl = webBaseUrl,
      canManage = canManage,
      isVisible = feed != null || (canManage && hasAudioContent),
      hasEpisodesWithoutPubDate = hasEpisodesWithoutPubDate,
    )
  }
}
