package dev.halim.shelfdroid.core.data.screen.editepisode

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
  fun run_whenUpdateSucceeds_updatesCacheAndPersistsReturnedTitle() = runTest {
    var capturedTitle: String? = null
    var cachedId: String? = null
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { itemId, episodeId, request ->
          capturedTitle = request.title
          Result.success(
            updatedItem(itemId = itemId, episodeId = episodeId, episodeTitle = "Updated title")
          )
        },
        updateCachedItem = { cachedId = it.id },
      )

    val result = runner.run(state(title = "Updated title", persistedTitle = "Old title"))

    assertEquals("Updated title", capturedTitle)
    assertEquals("item-1", cachedId)
    assertEquals("Updated title", result.state.title)
    assertEquals("Updated title", result.state.persistedTitle)
    assertEquals(false, result.state.isSaving)
    assertEquals(listOf(GenericUiEvent.ShowSuccessSnackbar()), result.events)
  }

  @Test
  fun run_whenUpdateFails_emitsErrorAndClearsSavingState() = runTest {
    val runner =
      EditEpisodeSaveRunner(
        updateEpisode = { _, _, _ -> Result.failure(IllegalStateException("Patch failed")) }
      )

    val result = runner.run(state(title = "Updated title", persistedTitle = "Old title"))

    assertEquals("Updated title", result.state.title)
    assertEquals("Old title", result.state.persistedTitle)
    assertEquals(false, result.state.isSaving)
    assertEquals(listOf(GenericUiEvent.ShowErrorSnackbar("Patch failed")), result.events)
  }

  private fun state(
    title: String,
    persistedTitle: String,
  ) = EditEpisodeUiState(
    state = GenericState.Success,
    itemId = "item-1",
    episodeId = "ep-1",
    podcastTitle = "Podcast",
    title = title,
    persistedTitle = persistedTitle,
    isSaving = true,
  )

  private fun updatedItem(
    itemId: String,
    episodeId: String,
    episodeTitle: String,
  ) = LibraryItem(
    id = itemId,
    libraryId = "library-1",
    mediaType = "podcast",
    media =
      Podcast(
        libraryItemId = itemId,
        episodes = listOf(PodcastEpisode(id = episodeId, title = episodeTitle)),
      ),
  )
}
