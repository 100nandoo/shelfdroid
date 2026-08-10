package dev.halim.shelfdroid.core.data.catalog

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.halim.core.network.response.libraryitem.Enclosure
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.libraryitem.PodcastMetadata
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.database.MyDatabase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastLocalDataSourceTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun storesRetrievesAndDeletesPodcastMetadataWithoutEpisodeHistory() {
    AndroidSqliteDriver(MyDatabase.Schema, context).use { driver ->
      val database =
        DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter())
      val repository = PodcastLocalDataSource(database, Json)
      val podcast =
        Podcast(
          coverPath = "/library/podcasts/cover.jpg",
          tags = listOf("Tech", "Daily"),
          metadata =
            PodcastMetadata(
              title = "Podcast",
              author = "Author",
              description = "Description",
              feedUrl = "https://example.com/feed.xml",
              itunesId = "12345",
              language = "en",
              type = "episodic",
            ),
          episodes =
            listOf(
              PodcastEpisode(
                id = "episode-1",
                title = "Episode",
                enclosure = Enclosure(url = "https://example.com/episode.mp3"),
                audioTrack =
                  AudioTrack(
                    metadata = FileMetadata(filename = "episode.mp3"),
                    contentUrl = "https://example.com/content.mp3",
                  ),
              )
            ),
          autoDownloadEpisodes = true,
          autoDownloadSchedule = "0 0 * * *",
          lastEpisodeCheck = 1234L,
          maxEpisodesToKeep = 25,
          maxNewEpisodesToDownload = 3,
        )

      repository.insert("podcast-1", podcast)

      val stored = repository.byId("podcast-1")
      assertEquals("Podcast", stored?.metadata?.title)
      assertEquals("Author", stored?.metadata?.author)
      assertEquals("https://example.com/feed.xml", stored?.metadata?.feedUrl)
      assertEquals("12345", stored?.metadata?.itunesId)
      assertEquals(listOf("Tech", "Daily"), stored?.tags)
      assertEquals("/library/podcasts/cover.jpg", stored?.coverPath)
      assertEquals(true, stored?.autoDownloadEpisodes)
      assertEquals("0 0 * * *", stored?.autoDownloadSchedule)
      assertEquals(1234L, stored?.lastEpisodeCheck)
      assertEquals(25, stored?.maxEpisodesToKeep)
      assertEquals(3, stored?.maxNewEpisodesToDownload)
      assertEquals(emptyList<PodcastEpisode>(), stored?.episodes)

      repository.deleteById("podcast-1")

      assertNull(repository.byId("podcast-1"))
    }
  }
}
