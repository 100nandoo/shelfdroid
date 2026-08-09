package dev.halim.shelfdroid.core.data.response

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.AudioMetaTags
import dev.halim.core.network.response.libraryitem.BookChapter
import dev.halim.core.network.response.libraryitem.Enclosure
import dev.halim.core.network.response.libraryitem.FileMetadata
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.data.di.DatabaseModule
import dev.halim.shelfdroid.core.database.LibraryItemCatalog
import dev.halim.shelfdroid.core.database.LibraryItemEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PodcastEpisodeRepoTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()

  @Test
  fun sync_storesEpisodesForDirectLookup() {
    database { database ->
      val repository = PodcastEpisodeRepo(database, Json)

      repository.replace("podcast-1", listOf(episode("episode-1", "First episode")))

      val stored = repository.byId("episode-1")
      assertEquals("First episode", stored?.title)
      assertEquals("podcast-1", stored?.libraryItemId)
      assertEquals("https://example.com/episode.mp3", stored?.enclosure?.url)
      assertEquals("episode.mp3", stored?.audioTrack?.metadata?.filename)
      assertEquals("https://example.com/content.mp3", stored?.audioTrack?.contentUrl)
      assertEquals("audio-file-1", stored?.audioFile?.ino)
      assertEquals("audio-file.mp3", stored?.audioFile?.metadata?.filename)
      assertEquals("Chapter one", stored?.audioFile?.chapters?.single()?.title)
      assertEquals("Album", stored?.audioFile?.metaTags?.tagAlbum)
    }
  }

  @Test
  fun sync_replacesStaleEpisodesAfterReplacementIsAvailable() {
    database { database ->
      val repository = PodcastEpisodeRepo(database, Json)
      repository.replace("podcast-1", listOf(episode("episode-1", "Stale episode")))

      repository.replace("podcast-1", listOf(episode("episode-2", "Fresh episode")))

      assertNull(repository.byId("episode-1"))
      assertEquals("Fresh episode", repository.byId("episode-2")?.title)
    }
  }

  @Test
  fun byLibraryItemId_returnsEpisodesForSelectedPodcast() {
    database { database ->
      val repository = PodcastEpisodeRepo(database, Json)
      repository.replace(
        "podcast-1",
        listOf(episode("episode-1", "First episode"), episode("episode-2", "Second episode")),
      )
      repository.replace("podcast-2", listOf(episode("episode-3", "Third episode")))

      assertEquals(
        setOf("episode-1", "episode-2"),
        repository.byLibraryItemId("podcast-1").map { episode -> episode.id }.toSet(),
      )
    }
  }

  @Test
  fun catalog_reportsPodcastEpisodeTotalWithoutEpisodePayload() {
    database { database ->
      database.libraryItemEntityQueries.insert(
        LibraryItemEntity(
          id = "podcast-1",
          libraryId = "library-1",
          author = "Author",
          title = "Podcast",
          description = "",
          cover = "cover",
          updatedAt = 0,
          rssFeed = null,
          isBook = 0,
          inoId = "",
          duration = "",
          addedAt = 1,
        )
      )
      PodcastEpisodeRepo(database, Json)
        .replace(
          "podcast-1",
          listOf(episode("episode-1", "First"), episode("episode-2", "Second")),
        )

      assertEquals(
        LibraryItemCatalog(
          id = "podcast-1",
          libraryId = "library-1",
          author = "Author",
          title = "Podcast",
          cover = "cover",
          isBook = 0,
          addedAt = 1,
          episodeCount = 2,
        ),
        database.libraryItemEntityQueries.libraryItemCatalog().executeAsOne(),
      )
    }
  }

  private fun episode(id: String, title: String) =
    PodcastEpisode(
      id = id,
      title = title,
      enclosure = Enclosure(url = "https://example.com/episode.mp3"),
      audioTrack =
        AudioTrack(
          metadata = FileMetadata(filename = "episode.mp3"),
          contentUrl = "https://example.com/content.mp3",
        ),
      audioFile =
        AudioFile(
          ino = "audio-file-1",
          metadata = FileMetadata(filename = "audio-file.mp3"),
          chapters = listOf(BookChapter(id = 1, start = 1.5, end = 2.5, title = "Chapter one")),
          metaTags = AudioMetaTags(tagAlbum = "Album"),
        ),
    )

  private fun database(block: (MyDatabase) -> Unit) {
    AndroidSqliteDriver(MyDatabase.Schema, context).use { driver ->
      block(DatabaseModule.provideSqlDelightAppDatabase(driver, DatabaseModule.provideMapAdapter()))
    }
  }
}
