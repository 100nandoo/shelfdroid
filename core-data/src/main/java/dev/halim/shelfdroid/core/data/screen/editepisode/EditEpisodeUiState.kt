package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.shelfdroid.core.data.GenericState

data class EditEpisodeUiState(
  val state: GenericState = GenericState.Loading,
  val itemId: String = "",
  val episodeId: String = "",
  val podcastTitle: String = "",
  val title: String = "",
  val persistedTitle: String = "",
  val isSaving: Boolean = false,
) {
  fun canSave(): Boolean =
    state == GenericState.Success && isSaving.not() && title != persistedTitle
}
