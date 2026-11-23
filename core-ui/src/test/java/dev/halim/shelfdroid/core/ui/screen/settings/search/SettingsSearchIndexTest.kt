package dev.halim.shelfdroid.core.ui.screen.settings.search

import dev.halim.shelfdroid.core.ui.navigation.SettingsSearchDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchIndexTest {
  private val index =
    SettingsSearchIndex(
      listOf(
        SettingsSearchEntry(
          id = "playback-speed",
          destination = SettingsSearchDestination.PLAYER,
          title = "Playback speed",
          path = listOf("Player"),
          keywords = "rate",
        ),
        SettingsSearchEntry(
          id = "speed-retention",
          destination = SettingsSearchDestination.PLAYBACK,
          title = "Keep playback speed",
          path = listOf("Playback", "When switching books"),
          aliases = listOf("remember speed"),
        ),
        SettingsSearchEntry(
          id = "timer",
          destination = SettingsSearchDestination.NOTIFICATION,
          title = "Sleep timer mode",
          path = listOf("Notification"),
          keywords = "timer notification",
        ),
      )
    )

  @Test
  fun all_query_words_match_in_any_order_and_punctuation_is_normalized() {
    assertTrue(index.search("SPE, play").any { it.id == "playback-speed" })
  }

  @Test
  fun title_prefix_and_aliases_match_without_fuzzy_matching() {
    assertTrue(index.search("play spe").any { it.id == "playback-speed" })
    assertEquals(listOf("speed-retention"), index.search("remember speed").map { it.id })
    assertTrue(index.search("playbak speed").isEmpty())
  }

  @Test
  fun exact_title_ranks_before_keyword_and_path_matches() {
    assertEquals(
      listOf("playback-speed", "speed-retention"),
      index.search("playback speed").take(2).map { it.id },
    )
  }

  @Test
  fun tied_scores_keep_the_index_order() {
    val tiedIndex =
      SettingsSearchIndex(
        listOf(
          SettingsSearchEntry(
            id = "first",
            destination = SettingsSearchDestination.PLAYER,
            title = "Equal result",
            path = listOf("Player"),
          ),
          SettingsSearchEntry(
            id = "second",
            destination = SettingsSearchDestination.PLAYER,
            title = "Equal result",
            path = listOf("Player"),
          ),
        )
      )

    val expected = listOf("first", "second")
    assertEquals(expected, tiedIndex.search("equal result").map { it.id })
    assertEquals(expected, tiedIndex.search("equal result").map { it.id })
  }

  @Test
  fun blank_queries_return_no_results() {
    assertTrue(index.search("   !!! ").isEmpty())
  }
}
