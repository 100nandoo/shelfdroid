package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.request.UpdatePodcastEpisodeRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EditEpisodeSaveRunnerTest {

  @Test
  fun run_whenUpdateSucceeds_updatesCacheAndPersistsReturnedDetails() = runTest {
    var capturedRequest: UpdatePodcastEpisodeRequest? = null
    var cachedId: String? = null
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { itemId, episodeId, request ->
          capturedRequest = request
          Result.success(
            updatedItem(
              itemId = itemId,
              episodeId = episodeId,
              episodeTitle = "Updated title",
              episodeSubtitle = "Updated subtitle",
              episodeDescription = "<p>Updated description</p>",
            )
          )
        },
        updateCachedItem = { cachedId = it.id },
      )

    val result =
      runner.run(
        state(
          details =
            EpisodeDetailsForm(
              title = "Updated title",
              subtitle = "Updated subtitle",
              description = "<p>Updated description</p>",
            ),
          originalDetails = EpisodeDetailsForm(title = "Old title"),
        )
      )

    assertEquals("Updated title", capturedRequest?.title)
    assertEquals("Updated subtitle", capturedRequest?.subtitle)
    assertEquals("<p>Updated description</p>", capturedRequest?.description)
    assertEquals("item-1", cachedId)
    assertEquals("Updated title", result.state.details.title)
    assertEquals("Updated subtitle", result.state.details.subtitle)
    assertEquals("<p>Updated description</p>", result.state.details.description)
    assertEquals(result.state.details, result.state.originalDetails)
    assertEquals(false, result.state.isSaving)
    assertEquals(listOf(GenericUiEvent.ShowSuccessSnackbar()), result.events)
  }

  @Test
  fun run_whenUpdateFails_emitsErrorAndClearsSavingState() = runTest {
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { _, _, _ -> Result.failure(IllegalStateException("Patch failed")) }
      )

    val result =
      runner.run(
        state(
          details = EpisodeDetailsForm(title = "Updated title"),
          originalDetails = EpisodeDetailsForm(title = "Old title"),
        )
      )

    assertEquals("Updated title", result.state.details.title)
    assertEquals("Old title", result.state.originalDetails.title)
    assertEquals(false, result.state.isSaving)
    assertEquals(listOf(GenericUiEvent.ShowErrorSnackbar("Patch failed")), result.events)
  }

  @Test
  fun run_whenNothingChanged_emitsNoOpFeedbackWithoutCallingPatch() = runTest {
    var patchCalls = 0
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { _, _, _ ->
          patchCalls += 1
          Result.failure(IllegalStateException("Should not patch"))
        }
      )

    val result =
      runner.run(
        state(
          details = EpisodeDetailsForm(title = "Same title"),
          originalDetails = EpisodeDetailsForm(title = "Same title"),
        )
      )

    assertEquals(0, patchCalls)
    assertEquals(false, result.state.isSaving)
    assertEquals(
      listOf(GenericUiEvent.ShowPlainSnackbar("No updates necessary")),
      result.events,
    )
  }

  @Test
  fun run_whenPublishedDateChanges_persistsPubDateAndPublishedAt() = runTest {
    var capturedRequest: UpdatePodcastEpisodeRequest? = null
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { _, _, request ->
          capturedRequest = request
          Result.success(updatedItem(itemId = "item-1", episodeId = "ep-1"))
        }
      )

    val publishedAtMillis = 1781818200000L
    runner.run(
      state(
        details = EpisodeDetailsForm(title = "Title", publishedAtMillis = publishedAtMillis),
        originalDetails = EpisodeDetailsForm(title = "Title"),
      )
    )

    assertEquals(publishedAtMillis, capturedRequest?.publishedAt)
    assertEquals(EditEpisodeMapper.formatPubDate(publishedAtMillis), capturedRequest?.pubDate)
  }

  private fun state(
    details: EpisodeDetailsForm,
    originalDetails: EpisodeDetailsForm,
  ) =
    EditEpisodeUiState(
      state = GenericState.Success,
      itemId = "item-1",
      episodeId = "ep-1",
      podcastTitle = "Podcast",
      details = details,
      originalDetails = originalDetails,
      isSaving = true,
    )

  private fun updatedItem(
    itemId: String,
    episodeId: String,
    episodeTitle: String = "Title",
    episodeSubtitle: String = "",
    episodeDescription: String = "",
  ) =
    LibraryItem(
      id = itemId,
      libraryId = "library-1",
      mediaType = "podcast",
      media =
        Podcast(
          libraryItemId = itemId,
          episodes =
            listOf(
              PodcastEpisode(
                id = episodeId,
                title = episodeTitle,
                subtitle = episodeSubtitle,
                description = episodeDescription,
              )
            ),
        ),
    )
}
