package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import dev.halim.core.network.response.libraryitem.AudioFile
import dev.halim.core.network.response.libraryitem.Enclosure
import dev.halim.core.network.response.libraryitem.PodcastEpisode
import dev.halim.core.network.response.play.AudioTrack
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.PodcastEpisodeEntity
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PodcastEpisodeRepo @Inject constructor(
  db: MyDatabase,
  private val json: Json,
) {

  private val queries = db.podcastEpisodeEntityQueries

  fun byId(id: String): PodcastEpisode? =
    queries.byId(id).executeAsOneOrNull()?.let(::toPodcastEpisode)

  fun flowById(id: String): Flow<PodcastEpisode?> =
    queries
      .byId(id)
      .asFlow()
      .mapToOneOrNull(Dispatchers.IO)
      .map { entity -> entity?.let(::toPodcastEpisode) }

  fun byLibraryItemId(libraryItemId: String): List<PodcastEpisode> =
    queries.byLibraryItemId(libraryItemId).executeAsList().map(::toPodcastEpisode)

  fun flowByLibraryItemId(libraryItemId: String): Flow<List<PodcastEpisode>> =
    queries
      .byLibraryItemId(libraryItemId)
      .asFlow()
      .mapToList(Dispatchers.IO)
      .map { entities -> entities.map(::toPodcastEpisode) }

  fun replace(libraryItemId: String, episodes: List<PodcastEpisode>) {
    queries.transaction {
      queries.deleteByLibraryItemId(libraryItemId)
      episodes.forEach { episode ->
        queries.insert(
          PodcastEpisodeEntity(
            id = episode.id,
            libraryItemId = libraryItemId,
            episodeIndex = episode.index?.toLong(),
            season = episode.season,
            episode = episode.episode,
            episodeType = episode.episodeType,
            title = episode.title,
            subtitle = episode.subtitle,
            description = episode.description,
            enclosureUrl = episode.enclosure?.url,
            enclosureType = episode.enclosure?.type,
            enclosureLength = episode.enclosure?.length,
            pubDate = episode.pubDate,
            audioFile = json.encodeToString(episode.audioFile),
            audioTrack = json.encodeToString(episode.audioTrack),
            publishedAt = episode.publishedAt,
            addedAt = episode.addedAt,
            updatedAt = episode.updatedAt,
          ),
        )
      }
    }
  }

  fun deleteByIds(ids: Set<String>) {
    if (ids.isNotEmpty()) queries.deleteByIds(ids)
  }

  fun deleteByLibraryItemId(libraryItemId: String) {
    queries.deleteByLibraryItemId(libraryItemId)
  }

  private fun toPodcastEpisode(entity: PodcastEpisodeEntity): PodcastEpisode =
    PodcastEpisode(
      libraryItemId = entity.libraryItemId,
      id = entity.id,
      index = entity.episodeIndex?.toInt(),
      season = entity.season,
      episode = entity.episode,
      episodeType = entity.episodeType,
      title = entity.title,
      subtitle = entity.subtitle,
      description = entity.description,
      enclosure = entity.toEnclosure(),
      pubDate = entity.pubDate,
      audioFile = json.decodeFromString(entity.audioFile),
      audioTrack = json.decodeFromString(entity.audioTrack),
      publishedAt = entity.publishedAt,
      addedAt = entity.addedAt,
      updatedAt = entity.updatedAt,
    )

  private fun PodcastEpisodeEntity.toEnclosure(): Enclosure? =
    enclosureUrl?.let { Enclosure(url = it, type = enclosureType.orEmpty(), length = enclosureLength.orEmpty()) }
}
