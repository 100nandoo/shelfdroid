package dev.halim.shelfdroid.core.ui.screen.podcast

internal data class PodcastInteractionState(
  val isSelectionMode: Boolean = false,
  val selectedEpisodeIds: Set<String> = emptySet(),
  val actionSheetEpisodeId: String? = null,
) {
  fun openEpisodeActions(
    episodeId: String,
    canEdit: Boolean,
    canDelete: Boolean,
  ): PodcastInteractionState {
    if (isSelectionMode || (!canEdit && !canDelete)) return this
    return copy(actionSheetEpisodeId = episodeId)
  }

  fun dismissEpisodeActions(): PodcastInteractionState = copy(actionSheetEpisodeId = null)

  fun setSelectionMode(
    enabled: Boolean,
    episodeId: String,
    autoSelectedIds: Set<String>,
  ): PodcastInteractionState {
    return if (enabled) {
      copy(
        isSelectionMode = true,
        selectedEpisodeIds = selectedEpisodeIds + episodeId + autoSelectedIds,
        actionSheetEpisodeId = null,
      )
    } else {
      copy(
        isSelectionMode = false,
        selectedEpisodeIds = emptySet(),
        actionSheetEpisodeId = null,
      )
    }
  }

  fun toggleSelection(episodeId: String): PodcastInteractionState {
    val updatedIds =
      if (selectedEpisodeIds.contains(episodeId)) {
        selectedEpisodeIds - episodeId
      } else {
        selectedEpisodeIds + episodeId
      }
    return copy(selectedEpisodeIds = updatedIds)
  }

  fun startDeleteSelectionFromActions(
    episodeId: String,
    autoSelectedIds: Set<String>,
  ): PodcastInteractionState =
    dismissEpisodeActions().setSelectionMode(true, episodeId, autoSelectedIds)
}
