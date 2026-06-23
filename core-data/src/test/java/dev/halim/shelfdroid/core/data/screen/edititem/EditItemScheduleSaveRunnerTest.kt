package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.UpdateLibraryItemMediaResponse
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class EditItemScheduleSaveRunnerTest {

  @Test
  fun run_whenCronIsInvalid_setsInlineError_andSkipsUpdate() = kotlinx.coroutines.test.runTest {
    var updateCalled = false
    val runner =
      runner(
        validateCron = { Result.failure(httpException(400, "Invalid cron expression")) },
        updateSchedule = { _, _ ->
          updateCalled = true
          Result.success(UpdateLibraryItemMediaResponse(updated = "1"))
        },
      )

    val result = runner.run(state(schedule = enabledSchedule(cronExpression = "bad cron")))

    assertEquals("Invalid cron expression", result.state.scheduleCronError)
    assertEquals(false, result.state.isSaving)
    assertEquals(emptyList<GenericUiEvent>(), result.events)
    assertEquals(false, updateCalled)
  }

  @Test
  fun run_whenValidationFailsForTransport_showsSnackbar_andKeepsInlineErrorClear() =
    kotlinx.coroutines.test.runTest {
      var updateCalled = false
      val runner =
        runner(
          validateCron = { Result.failure(IllegalStateException("Offline")) },
          updateSchedule = { _, _ ->
            updateCalled = true
            Result.success(UpdateLibraryItemMediaResponse(updated = "1"))
          },
        )

      val result = runner.run(state(schedule = enabledSchedule()))

      assertNull(result.state.scheduleCronError)
      assertEquals(false, result.state.isSaving)
      assertEquals(listOf(GenericUiEvent.ShowErrorSnackbar("Offline")), result.events)
      assertEquals(false, updateCalled)
    }

  @Test
  fun run_validatesBeforeSaving_andPreservesUnsavedNonScheduleEdits() =
    kotlinx.coroutines.test.runTest {
      val calls = mutableListOf<String>()
      var capturedRequest: UpdateLibraryItemMediaRequest? = null
      val runner =
        runner(
          validateCron = {
            calls += "validate:$it"
            Result.success(Unit)
          },
          updateSchedule = { _, request ->
            calls += "update"
            capturedRequest = request
            Result.success(UpdateLibraryItemMediaResponse(updated = "1"))
          },
        )

      val result =
        runner.run(
          state(
            details = DetailsForm(title = "Unsaved title"),
            originalDetails = DetailsForm(title = "Saved title"),
            schedule = enabledSchedule(cronExpression = "15 23 * * *"),
            originalSchedule = enabledSchedule(cronExpression = "0 1 * * 1"),
          )
        )

      assertEquals(listOf("validate:15 23 * * *", "update"), calls)
      assertEquals("Unsaved title", result.state.details.title)
      assertEquals("Saved title", result.state.originalDetails.title)
      assertEquals(EditItemTab.Schedule, result.state.currentTab)
      assertEquals(result.state.schedule, result.state.originalSchedule)
      assertEquals("15 23 * * *", result.state.originalSchedule.cronExpression)
      assertEquals(false, result.state.isSaving)
      assertEquals(listOf(GenericUiEvent.ShowSuccessSnackbar()), result.events)
      assertEquals(true, capturedRequest?.autoDownloadEpisodes)
      assertEquals("15 23 * * *", capturedRequest?.autoDownloadSchedule)
      assertEquals(0, capturedRequest?.maxEpisodesToKeep)
      assertEquals(3, capturedRequest?.maxNewEpisodesToDownload)
    }

  @Test
  fun run_whenDisabled_persistsLimitsWithoutRewritingStoredCronInFallbackBaseline() =
    kotlinx.coroutines.test.runTest {
      var validated = false
      var capturedRequest: UpdateLibraryItemMediaRequest? = null
      val runner =
        runner(
          validateCron = {
            validated = true
            Result.success(Unit)
          },
          updateSchedule = { _, request ->
            capturedRequest = request
            Result.success(UpdateLibraryItemMediaResponse(updated = "1"))
          },
        )

      val result =
        runner.run(
          state(
            schedule =
              PodcastScheduleForm(
                autoDownloadEpisodes = false,
                cronExpression = "30 6 * * *",
                maxEpisodesToKeepInput = "",
                maxNewEpisodesToDownloadInput = "9",
              ),
            originalSchedule =
              PodcastScheduleForm(
                autoDownloadEpisodes = true,
                cronExpression = "0 1 * * 1",
                maxEpisodesToKeepInput = "0",
                maxNewEpisodesToDownloadInput = "3",
              ),
          )
        )

      assertEquals(false, validated)
      assertEquals(false, result.state.originalSchedule.autoDownloadEpisodes)
      assertEquals("0 1 * * 1", result.state.originalSchedule.cronExpression)
      assertEquals("0", result.state.originalSchedule.maxEpisodesToKeepInput)
      assertEquals("9", result.state.originalSchedule.maxNewEpisodesToDownloadInput)
      assertEquals(false, capturedRequest?.autoDownloadEpisodes)
      assertNull(capturedRequest?.autoDownloadSchedule)
      assertEquals(0, capturedRequest?.maxEpisodesToKeep)
      assertEquals(9, capturedRequest?.maxNewEpisodesToDownload)
    }

  @Test
  fun run_whenResponseIncludesLibraryItem_usesServerScheduleAndUpdatesCache() =
    kotlinx.coroutines.test.runTest {
      var cachedItemId: String? = null
      val runner =
        runner(
          updateSchedule = { _, _ ->
            Result.success(
              UpdateLibraryItemMediaResponse(
                updated = "1",
                libraryItem =
                  podcastItem(
                    id = "podcast-1",
                    autoDownloadEpisodes = true,
                    autoDownloadSchedule = "10 4 * * *",
                    maxEpisodesToKeep = 7,
                    maxNewEpisodesToDownload = 2,
                  ),
              )
            )
          },
          updateCachedItem = { cachedItemId = it.id },
        )

      val result = runner.run(state(schedule = enabledSchedule()))

      assertEquals("podcast-1", cachedItemId)
      assertEquals("10 4 * * *", result.state.originalSchedule.cronExpression)
      assertEquals("7", result.state.originalSchedule.maxEpisodesToKeepInput)
      assertEquals("2", result.state.originalSchedule.maxNewEpisodesToDownloadInput)
    }

  @Test
  fun run_whenSimpleBuilderOwnsCron_skipsValidation_andPreservesSimpleMode() =
    kotlinx.coroutines.test.runTest {
      var validated = false
      var updateCalled = false
      val runner =
        runner(
          validateCron = {
            validated = true
            Result.success(Unit)
          },
          updateSchedule = { _, _ ->
            updateCalled = true
            Result.success(UpdateLibraryItemMediaResponse(updated = "1"))
          },
        )

      val result =
        runner.run(
          state(
            schedule = enabledSchedule(),
            scheduleMode = PodcastScheduleMode.Simple,
            simpleScheduleBuilder =
              PodcastScheduleSimpleBuilder(
                interval = PodcastScheduleSimpleInterval.Daily,
                selectedHour = "23",
                selectedMinute = "15",
              ),
          )
        )

      assertEquals(false, validated)
      assertEquals(true, updateCalled)
      assertEquals(PodcastScheduleMode.Simple, result.state.scheduleMode)
      assertEquals("15 23 * * *", result.state.originalSchedule.cronExpression)
      assertEquals(listOf(GenericUiEvent.ShowSuccessSnackbar()), result.events)
    }

  private fun runner(
    validateCron: suspend (String) -> Result<Unit> = { Result.success(Unit) },
    updateSchedule:
      suspend (String, UpdateLibraryItemMediaRequest) -> Result<UpdateLibraryItemMediaResponse> =
      { _, _ -> Result.success(UpdateLibraryItemMediaResponse(updated = "1")) },
    updateCachedItem: (LibraryItem) -> Unit = {},
  ) = EditItemScheduleSaveRunner(validateCron, updateSchedule, updateCachedItem)

  private fun state(
    details: DetailsForm = DetailsForm(),
    originalDetails: DetailsForm = DetailsForm(),
    schedule: PodcastScheduleForm = enabledSchedule(),
    originalSchedule: PodcastScheduleForm = enabledSchedule(cronExpression = "0 1 * * 1"),
    scheduleMode: PodcastScheduleMode = PodcastScheduleMode.Advanced,
    simpleScheduleBuilder: PodcastScheduleSimpleBuilder = PodcastScheduleSimpleBuilder(),
  ) =
    EditItemUiState(
      state = GenericState.Success,
      itemId = "podcast-1",
      mediaKind = EditItemMediaKind.Podcast,
      currentTab = EditItemTab.Schedule,
      details = details,
      originalDetails = originalDetails,
      schedule = schedule,
      originalSchedule = originalSchedule,
      scheduleMode = scheduleMode,
      simpleScheduleBuilder = simpleScheduleBuilder,
      isSaving = true,
    )

  private fun enabledSchedule(cronExpression: String = "15 23 * * *") =
    PodcastScheduleForm(
      autoDownloadEpisodes = true,
      cronExpression = cronExpression,
      maxEpisodesToKeepInput = "0",
      maxNewEpisodesToDownloadInput = "3",
    )

  private fun podcastItem(
    id: String,
    autoDownloadEpisodes: Boolean,
    autoDownloadSchedule: String,
    maxEpisodesToKeep: Int,
    maxNewEpisodesToDownload: Int,
  ) =
    LibraryItem(
      id = id,
      mediaType = "podcast",
      media =
        Podcast(
          libraryItemId = id,
          coverPath = null,
          tags = emptyList(),
          autoDownloadEpisodes = autoDownloadEpisodes,
          autoDownloadSchedule = autoDownloadSchedule,
          maxEpisodesToKeep = maxEpisodesToKeep,
          maxNewEpisodesToDownload = maxNewEpisodesToDownload,
        ),
    )

  private fun httpException(code: Int, body: String): HttpException =
    HttpException(
      Response.error<String>(
        code,
        body.toResponseBody("text/plain".toMediaType()),
      )
    )
}
