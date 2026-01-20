package dev.halim.shelfdroid.core.data.screen.podcast

import dev.halim.shelfdroid.core.DownloadUiState
import dev.halim.shelfdroid.core.Prefs
import dev.halim.shelfdroid.core.data.GenericState

data class PodcastUiState(
  val state: GenericState = GenericState.Loading,
  val apiState: PodcastApiState = PodcastApiState.Idle,
  val isSelectionMode: Boolean = false,
  val selectedEpisodeIds: Set<String> = emptySet(),
  val author: String = "",
  val title: String = "",
  val cover: String = "",
  val description: String = "",
  val canAddEpisode: Boolean = false,
  val canDeleteEpisode: Boolean = false,
  val episodes: List<Episode> = emptyList(),
  val prefs: Prefs = Prefs(),
)

data class Episode(
  val episodeId: String = "",
  val title: String = "",
  val publishedAt: String = "",
  val progress: Float = 0f,
  val isFinished: Boolean = false,
  val isPlaying: Boolean = false,
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
}
