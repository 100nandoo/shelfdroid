package dev.halim.shelfdroid.core.data.screen.rssfeeds

import dev.halim.core.network.ApiService
import dev.halim.shelfdroid.core.AudiobookshelfBaseUrl
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import dev.halim.shelfdroid.helper.Helper
import javax.inject.Inject

class RssFeedsRepository
@Inject
constructor(private val api: ApiService, private val helper: Helper) {

  suspend fun loadFeeds(): RssFeedsUiState {
    val response =
      api.rssFeeds().getOrElse {
        return RssFeedsUiState(state = GenericState.Failure(it.message))
      }

    return RssFeedsUiState(
      state = GenericState.Success,
      apiState = RssFeedsApiState.Idle,
      feeds =
        RssFeedsMapper.map(response, currentWebBaseUrl()) {
          helper.toReadableDate(it, includeTime = true)
        },
    )
  }

  suspend fun closeFeed(feedId: String, uiState: RssFeedsUiState): RssFeedsUiState {
    api.closeRssFeed(feedId).getOrElse {
      return uiState.copy(apiState = RssFeedsApiState.CloseFailure(it.message))
    }

    return uiState.copy(
      apiState = RssFeedsApiState.CloseSuccess,
      feeds = uiState.feeds.filterNot { it.id == feedId },
    )
  }

  private fun currentWebBaseUrl(): String =
    AudiobookshelfBaseUrl.parse(DataStoreManager.BASE_URL)?.value
      ?: AudiobookshelfBaseUrl.DEFAULT_VALUE
}
