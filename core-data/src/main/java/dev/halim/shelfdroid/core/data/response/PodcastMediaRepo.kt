package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.response.libraryitem.Podcast
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.PodcastEntity
import javax.inject.Inject
import kotlinx.serialization.json.Json

class PodcastMediaRepo @Inject constructor(
  db: MyDatabase,
  private val json: Json,
) {

  private val queries = db.podcastEntityQueries

  fun byId(libraryItemId: String): Podcast? =
    queries.byLibraryItemId(libraryItemId).executeAsOneOrNull()?.let(::toPodcast)

  fun insert(libraryItemId: String, podcast: Podcast) {
    queries.insert(
      PodcastEntity(
        libraryItemId = libraryItemId,
        media = json.encodeToString(podcast.copy(libraryItemId = libraryItemId, episodes = emptyList())),
      )
    )
  }

  fun deleteById(libraryItemId: String) {
    queries.deleteByLibraryItemId(libraryItemId)
  }

  private fun toPodcast(entity: PodcastEntity): Podcast = json.decodeFromString(entity.media)
}
