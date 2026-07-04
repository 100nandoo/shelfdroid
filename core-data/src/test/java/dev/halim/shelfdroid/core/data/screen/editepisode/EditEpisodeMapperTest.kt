package dev.halim.shelfdroid.core.data.screen.editepisode

import dev.halim.core.network.response.libraryitem.Enclosure
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Test

class EditEpisodeMapperTest {

  @Test
  fun mapState_mapsEditableEpisodeFields() {
    val episode =
      PodcastEpisode(
        id = "ep-1",
        season = "3",
        episode = "9",
        episodeType = "bonus",
        title = "Episode title",
        subtitle = "Episode subtitle",
        description = "<p>Description</p>",
        enclosure = Enclosure(url = "https://example.com/episode.mp3"),
        pubDate = "Fri, 18 Jun 2026 03:30:00+0800",
      )

    val state =
      EditEpisodeMapper.mapState(
        itemId = "item-1",
        episodeId = "ep-1",
        podcastTitle = "Podcast",
        episode = episode,
      )

    assertEquals("Podcast", state.podcastTitle)
    assertEquals("3", state.details.season)
    assertEquals("9", state.details.episode)
    assertEquals("bonus", state.details.episodeType)
    assertEquals("Episode title", state.details.title)
    assertEquals("Episode subtitle", state.details.subtitle)
    assertEquals("<p>Description</p>", state.details.description)
    assertEquals("https://example.com/episode.mp3", state.details.enclosureUrl)
    assertEquals(
      EditEpisodeMapper.parsePublishedAtMillis(episode.pubDate, episode.publishedAt),
      state.details.publishedAtMillis,
    )
    assertEquals("Episode title", state.match.searchTerm)
    assertEquals(state.details, state.originalDetails)
  }
}
