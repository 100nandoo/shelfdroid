package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.shelfdroid.core.data.GenericState

data class EditEpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val itemId: String = "",
  val episodeId: String = "",
  val podcastTitle: String = "",
  val currentTab: EditEpisodeTab = EditEpisodeTab.Details,
  val details: EpisodeDetailsForm = EpisodeDetailsForm(),
  val originalDetails: EpisodeDetailsForm = EpisodeDetailsForm(),
  val match: EpisodeMatchState = EpisodeMatchState(),
  val isSaving: Boolean = false,
) {
  fun canSave(): Boolean = state == GenericState.Success && isSaving.not()
}

enum class EditEpisodeTab {
  Details,
  Match,
}

data class EpisodeDetailsForm(
  val season: String = "",
  val episode: String = "",
  val episodeType: String = "",
  val publishedAtMillis: Long? = null,
  val title: String = "",
  val subtitle: String = "",
  val description: String = "",
  val enclosureUrl: String = "",
)

data class EpisodeMatchState(
  val searchTerm: String = "",
  val results: List<EpisodeMatchResultRow> = emptyList(),
  val isSearching: Boolean = false,
  val hasSearched: Boolean = false,
  val errorMessage: String? = null,
)

data class EpisodeMatchResultRow(
  val season: String = "",
  val episode: String = "",
  val episodeType: String = "",
  val title: String = "",
  val subtitle: String = "",
  val description: String = "",
  val enclosureUrl: String = "",
  val enclosureType: String = "",
  val enclosureLength: String = "",
  val pubDate: String = "",
  val publishedAtMillis: Long? = null,
)
