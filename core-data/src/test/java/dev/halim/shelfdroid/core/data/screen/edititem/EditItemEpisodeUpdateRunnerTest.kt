package dev.halim.shelfdroid.core.data.screen.edititem

import dev.halim.core.network.request.UpdateLibraryItemMediaRequest
import dev.halim.core.network.response.LibraryItem
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.data.GenericState
import dev.halim.shelfdroid.core.data.GenericUiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditItemEpisodeUpdateRunnerTest {

  @Test
  fun run_whenCutoffChanged_updatesCutoffBeforeCheckingEpisodes() =
    kotlinx.coroutines.test.runTest {
      val calls = mutableListOf<String>()
      val expectedCutoff = cutoffMillis(2026, 6, 18, 21, 30)
      val runner =
        runner(
          updateEpisodeCutoff = { _, request ->
            calls += "cutoff:${request.lastEpisodeCheck}"
            Result.success(Unit)
          },
          checkNewEpisodes = { _, limit ->
            calls += "check:$limit"
            Result.success(2)
          },
        )

      runner.run(
        state(
          selectedCutoffMillis = expectedCutoff,
          persistedCutoffMillis = 0L,
        )
      )

      assertEquals(listOf("cutoff:$expectedCutoff", "check:3"), calls)
    }

  @Test
  fun run_whenNegativeLimit_resetsTo3_emitsError_andContinues() =
    kotlinx.coroutines.test.runTest {
      var checkedLimit = -1
      val runner =
        runner(
          checkNewEpisodes = { _, limit ->
            checkedLimit = limit
            Result.success(0)
          }
        )

      val result = runner.run(state(limitInput = "-4"))

      assertEquals(3, checkedLimit)
      assertEquals("3", result.state.episodeUpdate.limitInput)
      assertTrue(result.events.first() is GenericUiEvent.ShowErrorSnackbar)
    }

  @Test
  fun run_whenCutoffUpdateSucceedsButCheckFails_preservesPersistedCutoff() =
    kotlinx.coroutines.test.runTest {
      val runner =
        runner(
          updateEpisodeCutoff = { _, _ -> Result.success(Unit) },
          checkNewEpisodes = { _, _ -> Result.failure(IllegalStateException("Check failed")) },
        )
      val expectedCutoff = cutoffMillis(2026, 6, 18, 21, 30)

      val result =
        runner.run(state(selectedCutoffMillis = expectedCutoff, persistedCutoffMillis = 0L))

      assertEquals(expectedCutoff, result.state.episodeUpdate.persistedCutoffMillis)
      assertEquals(expectedCutoff, result.state.episodeUpdate.selectedCutoffMillis)
      assertEquals(false, result.state.episodeUpdate.isRunning)
      assertEquals(
        GenericUiEvent.ShowErrorSnackbar("Check failed"),
        result.events.last(),
      )
    }

  @Test
  fun run_whenNoNewEpisodes_showsInformationalFeedback() =
    kotlinx.coroutines.test.runTest {
      val runner = runner(checkNewEpisodes = { _, _ -> Result.success(0) })

      val result = runner.run(state())

      assertEquals(
        listOf(GenericUiEvent.ShowPlainSnackbar("No new episodes found.")),
        result.events,
      )
    }

  @Test
  fun run_whenEpisodesFound_refreshesAndKeepsEpisodesTab() =
    kotlinx.coroutines.test.runTest {
      var cacheUpdatedWith: String? = null
      val runner =
        runner(
          checkNewEpisodes = { _, _ -> Result.success(2) },
          reloadItem = { Result.success(refreshedItem("updated")) },
          mergeUpdated = { state, item ->
            state.copy(
              currentTab = EditItemTab.Episodes,
              state = GenericState.Success,
              itemId = item.id,
              episodes = listOf(EpisodeRow("updated", "Updated episode")),
            )
          },
          updateCachedItem = { cacheUpdatedWith = it.id },
        )

      val result = runner.run(state())

      assertEquals("updated", cacheUpdatedWith)
      assertEquals(EditItemTab.Episodes, result.state.currentTab)
      assertEquals(listOf("updated"), result.state.episodes.map { it.id })
      assertEquals(
        listOf(GenericUiEvent.ShowSuccessSnackbar("Found 2 new episodes.")),
        result.events,
      )
    }

  @Test
  fun run_allowsRetryAfterFailure() =
    kotlinx.coroutines.test.runTest {
      var attempts = 0
      val runner =
        runner(
          checkNewEpisodes = { _, _ ->
            attempts += 1
            if (attempts == 1) Result.failure(IllegalStateException("First failure"))
            else Result.success(1)
          }
        )

      val first = runner.run(state())
      val second =
        runner.run(
          first.state.copy(episodeUpdate = first.state.episodeUpdate.copy(isRunning = true))
        )

      assertEquals(false, first.state.episodeUpdate.isRunning)
      assertEquals(false, second.state.episodeUpdate.isRunning)
      assertEquals(GenericUiEvent.ShowErrorSnackbar("First failure"), first.events.last())
      assertEquals(
        GenericUiEvent.ShowSuccessSnackbar("Found 1 new episodes."),
        second.events.last(),
      )
    }

  private fun runner(
    updateEpisodeCutoff:
      suspend (itemId: String, request: UpdateLibraryItemMediaRequest) -> Result<Unit> =
      { _, _ ->
        Result.success(Unit)
      },
    checkNewEpisodes: suspend (itemId: String, limit: Int) -> Result<Int> = { _, _ ->
      Result.success(0)
    },
    reloadItem: suspend (itemId: String) -> Result<LibraryItem> = {
      Result.success(refreshedItem("item-1"))
    },
    mergeUpdated: (EditItemUiState, LibraryItem) -> EditItemUiState = { state, _ -> state },
    updateCachedItem: (LibraryItem) -> Unit = {},
  ) =
    EditItemEpisodeUpdateRunner(
      updateEpisodeCutoff = updateEpisodeCutoff,
      checkNewEpisodes = checkNewEpisodes,
      reloadItem = reloadItem,
      mergeUpdated = mergeUpdated,
      updateCachedItem = updateCachedItem,
    )

  private fun state(
    selectedCutoffMillis: Long? = null,
    persistedCutoffMillis: Long = 0L,
    limitInput: String = "3",
  ) =
    EditItemUiState(
      state = GenericState.Success,
      itemId = "item-1",
      mediaKind = EditItemMediaKind.Podcast,
      currentTab = EditItemTab.Episodes,
      episodeUpdate =
        EpisodeUpdateState(
          persistedCutoffMillis = persistedCutoffMillis,
          selectedCutoffMillis = selectedCutoffMillis,
          limitInput = limitInput,
          isRunning = true,
        ),
    )

  private fun cutoffMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
    java.util.Calendar.getInstance().run {
      set(java.util.Calendar.YEAR, year)
      set(java.util.Calendar.MONTH, month - 1)
      set(java.util.Calendar.DAY_OF_MONTH, day)
      set(java.util.Calendar.HOUR_OF_DAY, hour)
      set(java.util.Calendar.MINUTE, minute)
      set(java.util.Calendar.SECOND, 0)
      set(java.util.Calendar.MILLISECOND, 0)
      timeInMillis
    }

  private fun refreshedItem(id: String) =
    LibraryItem(
      id = id,
      mediaType = "podcast",
      media = Podcast(libraryItemId = id, coverPath = null, tags = emptyList()),
    )
}
