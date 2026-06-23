package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.UpdateLibraryItemMediaResponse
import dev.halim.shelfdroid.core.data.GenericUiEvent
import retrofit2.HttpException

internal data class EditItemScheduleSaveResult(
  val state: EditItemUiState,
  val events: List<GenericUiEvent> = emptyList(),
)

internal class EditItemScheduleSaveRunner(
  private val validateCron: suspend (expression: String) -> Result<Unit>,
  private val updateSchedule:
    suspend (itemId: String, request: UpdateLibraryItemMediaRequest) -> Result<UpdateLibraryItemMediaResponse>,
  private val updateCachedItem: (LibraryItem) -> Unit = {},
) {

  suspend fun run(state: EditItemUiState): EditItemScheduleSaveResult {
    val workingState = state.copy(scheduleCronError = null)
    val schedule = workingState.schedule
    val cronExpression = schedule.cronExpression.trim()

    if (workingState.shouldValidateScheduleCron()) {
      if (cronExpression.isBlank()) {
        return EditItemScheduleSaveResult(
          state = workingState.copy(isSaving = false, scheduleCronError = INVALID_CRON_EXPRESSION),
        )
      }

      validateCron(cronExpression).exceptionOrNull()?.let { error ->
        if (error is HttpException && error.code() == 400) {
          return EditItemScheduleSaveResult(
            state =
              workingState.copy(isSaving = false, scheduleCronError = INVALID_CRON_EXPRESSION),
          )
        }

        return EditItemScheduleSaveResult(
          state = workingState.copy(isSaving = false),
          events =
            listOf(
              GenericUiEvent.ShowErrorSnackbar(
                error.message.orEmpty().ifBlank { FAILED_TO_VALIDATE_CRON_EXPRESSION }
              )
            ),
        )
      }
    }

    val response =
      updateSchedule(workingState.itemId, buildScheduleUpdateRequest(schedule)).getOrElse { error ->
        return EditItemScheduleSaveResult(
          state = workingState.copy(isSaving = false),
          events =
            listOf(
              GenericUiEvent.ShowErrorSnackbar(
                error.message.orEmpty().ifBlank { FAILED_TO_SAVE_SCHEDULE }
              )
            ),
        )
      }

    val persistedSchedule =
      response.libraryItem?.let { item ->
        updateCachedItem(item)
        mapScheduleFromItem(item)
      } ?: successfulScheduleBaseline(workingState)
    val schedulePresentation =
      deriveSchedulePresentation(
        schedule = persistedSchedule,
        preferredMode = workingState.scheduleMode,
        currentBuilder = workingState.simpleScheduleBuilder,
      )

    return EditItemScheduleSaveResult(
      state =
        workingState.copy(
          currentTab = EditItemTab.Schedule,
          schedule = persistedSchedule,
          originalSchedule = persistedSchedule,
          scheduleMode = schedulePresentation.mode,
          simpleScheduleBuilder = schedulePresentation.simpleBuilder,
          isSaving = false,
          scheduleCronError = null,
        ),
      events = listOf(GenericUiEvent.ShowSuccessSnackbar()),
    )
  }
}

internal fun buildScheduleUpdateRequest(
  schedule: PodcastScheduleForm
): UpdateLibraryItemMediaRequest {
  val normalized = normalizeScheduleForm(schedule)
  return UpdateLibraryItemMediaRequest(
    autoDownloadEpisodes = normalized.autoDownloadEpisodes,
    autoDownloadSchedule = normalized.cronExpression.takeIf { normalized.autoDownloadEpisodes },
    maxEpisodesToKeep = normalizedNonNegativeInt(normalized.maxEpisodesToKeepInput),
    maxNewEpisodesToDownload = normalizedNonNegativeInt(normalized.maxNewEpisodesToDownloadInput),
  )
}

internal fun normalizeScheduleForm(schedule: PodcastScheduleForm): PodcastScheduleForm =
  schedule.copy(
    cronExpression = schedule.cronExpression.trim(),
    maxEpisodesToKeepInput = normalizedNonNegativeInt(schedule.maxEpisodesToKeepInput).toString(),
    maxNewEpisodesToDownloadInput =
      normalizedNonNegativeInt(schedule.maxNewEpisodesToDownloadInput).toString(),
  )

internal fun successfulScheduleBaseline(state: EditItemUiState): PodcastScheduleForm {
  val normalized = normalizeScheduleForm(state.schedule)
  return normalized.copy(
    cronExpression =
      if (normalized.autoDownloadEpisodes) normalized.cronExpression
      else state.originalSchedule.cronExpression.trim(),
  )
}

internal fun mapScheduleFromItem(item: LibraryItem): PodcastScheduleForm = EditItemMapper.mapMedia(item).schedule

private const val INVALID_CRON_EXPRESSION = "Invalid cron expression"
private const val FAILED_TO_VALIDATE_CRON_EXPRESSION = "Failed to validate cron expression"
private const val FAILED_TO_SAVE_SCHEDULE = "Failed to save schedule"
