package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.shelfdroid.core.data.GenericState

data class EditEpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val itemId: String = "",
  val episodeId: String = "",
  val podcastTitle: String = "",
  val details: EpisodeDetailsForm = EpisodeDetailsForm(),
  val originalDetails: EpisodeDetailsForm = EpisodeDetailsForm(),
  val isSaving: Boolean = false,
) {
  fun canSave(): Boolean = state == GenericState.Success && isSaving.not()
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
