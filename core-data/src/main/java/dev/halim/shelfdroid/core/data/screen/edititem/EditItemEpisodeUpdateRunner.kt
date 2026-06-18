package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.shelfdroid.core.data.GenericUiEvent

internal data class EditItemEpisodeUpdateResult(
  val state: EditItemUiState,
  val events: List<GenericUiEvent>,
)

internal class EditItemEpisodeUpdateRunner(
  private val updateEpisodeCutoff:
    suspend (itemId: String, request: UpdateLibraryItemMediaRequest) -> Result<Unit>,
  private val checkNewEpisodes: suspend (itemId: String, limit: Int) -> Result<Int>,
  private val reloadItem: suspend (itemId: String) -> Result<LibraryItem>,
  private val mergeUpdated: (EditItemUiState, LibraryItem) -> EditItemUiState,
  private val updateCachedItem: (LibraryItem) -> Unit,
) {

  suspend fun run(state: EditItemUiState): EditItemEpisodeUpdateResult {
    val normalizedLimit = normalizeLimit(state.episodeUpdate.limitInput)
    if (normalizedLimit == null) {
      return EditItemEpisodeUpdateResult(
        state = state.copy(episodeUpdate = state.episodeUpdate.copy(isRunning = false)),
        events = listOf(GenericUiEvent.ShowErrorSnackbar("Enter a valid episode limit.")),
      )
    }

    val stateWithLimit = state.copy(episodeUpdate = state.episodeUpdate.copy(limitInput = normalizedLimit.limitText))
    val cutoffResult = resolveCutoffMillis(stateWithLimit.episodeUpdate)
    if (cutoffResult is CutoffResolution.Invalid) {
      return EditItemEpisodeUpdateResult(
        state = stateWithLimit.copy(episodeUpdate = stateWithLimit.episodeUpdate.copy(isRunning = false)),
        events = listOf(GenericUiEvent.ShowErrorSnackbar(cutoffResult.message)),
      )
    }

    var workingState = stateWithLimit
    val events = mutableListOf<GenericUiEvent>()
    normalizedLimit.message?.let { events += GenericUiEvent.ShowErrorSnackbar(it) }

    val cutoffMillis = (cutoffResult as CutoffResolution.Valid).millis
    if (cutoffMillis != null && cutoffMillis != workingState.episodeUpdate.persistedCutoffMillis) {
      val cutoffUpdate =
        updateEpisodeCutoff(
          workingState.itemId,
          UpdateLibraryItemMediaRequest(lastEpisodeCheck = cutoffMillis),
        )
      if (cutoffUpdate.isFailure) {
        return EditItemEpisodeUpdateResult(
          state = workingState.copy(episodeUpdate = workingState.episodeUpdate.copy(isRunning = false)),
          events = events + GenericUiEvent.ShowErrorSnackbar(cutoffUpdate.exceptionOrNull()?.message.orEmpty()),
        )
      }

      workingState =
        workingState.copy(
          episodeUpdate =
            workingState.episodeUpdate.copy(
              persistedCutoffMillis = cutoffMillis,
              cutoffInput = formatEpisodeUpdateCutoffInput(cutoffMillis),
            )
        )
    }

    val checkResult = checkNewEpisodes(workingState.itemId, normalizedLimit.limit)
    if (checkResult.isFailure) {
      return EditItemEpisodeUpdateResult(
        state = workingState.copy(episodeUpdate = workingState.episodeUpdate.copy(isRunning = false)),
        events = events + GenericUiEvent.ShowErrorSnackbar(checkResult.exceptionOrNull()?.message.orEmpty()),
      )
    }

    val refreshedItem = reloadItem(workingState.itemId).getOrNull()
    if (refreshedItem != null) {
      updateCachedItem(refreshedItem)
      workingState = mergeUpdated(workingState, refreshedItem)
    }

    val episodeCount = checkResult.getOrThrow()
    val feedback =
      if (episodeCount == 0) {
        GenericUiEvent.ShowPlainSnackbar("No new episodes found.")
      } else {
        GenericUiEvent.ShowSuccessSnackbar("Found $episodeCount new episodes.")
      }

    return EditItemEpisodeUpdateResult(
      state = workingState.copy(episodeUpdate = workingState.episodeUpdate.copy(isRunning = false)),
      events = events + feedback,
    )
  }
}

private sealed interface CutoffResolution {
  data class Valid(val millis: Long?) : CutoffResolution

  data class Invalid(val message: String) : CutoffResolution
}

private fun resolveCutoffMillis(episodeUpdate: EpisodeUpdateState): CutoffResolution {
  val input = episodeUpdate.cutoffInput.trim()
  if (input.isBlank()) {
    return if (episodeUpdate.persistedCutoffMillis == 0L) CutoffResolution.Valid(null)
    else CutoffResolution.Invalid("Enter a complete episode update cutoff.")
  }

  val millis = parseEpisodeUpdateCutoffInput(input)
  return if (millis == null) {
    CutoffResolution.Invalid("Enter a valid episode update cutoff in YYYY-MM-DD HH:MM format.")
  } else {
    CutoffResolution.Valid(millis)
  }
}

private data class NormalizedLimit(val limit: Int, val limitText: String, val message: String? = null)

private fun normalizeLimit(limitInput: String): NormalizedLimit? {
  val parsed = limitInput.trim().toIntOrNull() ?: return null
  return if (parsed < 0) {
    NormalizedLimit(
      limit = 3,
      limitText = "3",
      message = "Episode limit cannot be negative. Reset to 3.",
    )
  } else {
    NormalizedLimit(limit = parsed, limitText = parsed.toString())
  }
}
