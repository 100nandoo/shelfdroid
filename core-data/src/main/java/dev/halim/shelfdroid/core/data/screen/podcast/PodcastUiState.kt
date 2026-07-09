package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.PlayPauseControlState
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.screen.rssfeeds.GeneratedRssFeedUiState

data class PodcastUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: PodcastApiState = PodcastApiState.Idle,
  val isSelectionMode: Boolean = false,
  val selectedEpisodeIds: Set<String> = emptySet(),
  val actionSheetEpisodeId: String? = null,
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val description: String = "",
  val canAddEpisode: Boolean = false,
  val canEditEpisode: Boolean = false,
  val canDeleteEpisode: Boolean = false,
  val generatedRssFeed: GeneratedRssFeedUiState = GeneratedRssFeedUiState(),
  val episodes: List<Episode> = emptyList(),
  val prefs: Prefs = Prefs(),
)

data class Episode(
  val episodeId: String = "",
  val title: String = "",
  val publishedAt: String = "",
  val progress: Float = 0f,
  val isFinished: Boolean = false,
  val playPause: PlayPauseControlState = PlayPauseControlState(enabled = true),
  val download: DownloadUiState = DownloadUiState(),
)

sealed interface PodcastApiState {
  data object Idle : PodcastApiState

  data object AddSuccess : PodcastApiState

  data class DeleteSuccess(val size: Int) : PodcastApiState

  data class AddFailure(val message: String) : PodcastApiState

  data class DeleteFailure(val message: String) : PodcastApiState

  data object AddLoading : PodcastApiState

  data object DeleteLoading : PodcastApiState

  data object OpenRssFeedLoading : PodcastApiState

  data object OpenRssFeedSuccess : PodcastApiState

  data class OpenRssFeedFailure(val message: String) : PodcastApiState

  data object CloseRssFeedLoading : PodcastApiState

  data object CloseRssFeedSuccess : PodcastApiState

  data class CloseRssFeedFailure(val message: String) : PodcastApiState
}
