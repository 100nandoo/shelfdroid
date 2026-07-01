package dev.halim.shelfdroid.core.data.screen.home

import dev.halim.shelfdroid.core.database.ProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeMapperTest {

  @Test
  fun podcastProgressLastUpdate_usesMostRecentEpisodeUpdateIncludingFinishedEpisodes() {
    val progresses =
      listOf(
        progressEntity(episodeId = "ep-old", lastUpdate = 100L, isFinished = 0L),
        progressEntity(episodeId = "ep-finished", lastUpdate = 300L, isFinished = 1L),
        progressEntity(episodeId = "ep-mid", lastUpdate = 200L, isFinished = 0L),
        progressEntity(episodeId = null, lastUpdate = 400L, isFinished = 0L),
      )

    assertEquals(300L, podcastProgressLastUpdate(progresses))
  }

  @Test
  fun podcastProgressLastUpdate_whenNoEpisodeProgress_returnsZero() {
    assertEquals(0L, podcastProgressLastUpdate(emptyList()))
  }

  private fun progressEntity(
    episodeId: String?,
    lastUpdate: Long,
    isFinished: Long,
  ): ProgressEntity {
    return ProgressEntity(
      id = "progress-$episodeId",
      libraryItemId = "podcast-1",
      episodeId = episodeId,
      mediaItemType = "podcastEpisode",
      progress = 0.0,
      duration = 0.0,
      currentTime = 0.0,
      isFinished = isFinished,
      lastUpdate = lastUpdate,
    )
  }
}
