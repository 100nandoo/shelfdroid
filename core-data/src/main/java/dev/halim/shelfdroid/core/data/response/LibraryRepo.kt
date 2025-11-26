package dev.halim.shelfdroid.core.data.response

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import dev.halim.core.network.ApiService
import dev.halim.core.network.response.Folder
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class LibraryRepo
@Inject
constructor(private val api: ApiService, db: MyDatabase, private val json: Json) {

  private val queries = db.libraryEntityQueries

  suspend fun remote() {
    val response = api.libraries().getOrNull()
    if (response != null) {
      val entities = convert(response)
      cleanup(entities)
      entities.forEach { entity -> queries.insert(entity) }
    }
  }

  fun byId(id: String): LibraryEntity? {
    return queries.byId(id).executeAsOneOrNull()
  }

  fun foldersByLibraryId(id: String): List<PodcastFolder> {
    val entity = byId(id)
    if (entity == null) return emptyList()
    val folders = runCatching { json.decodeFromString<List<Folder>>(entity.folders) }
    return folders.getOrNull()?.map { PodcastFolder(it) } ?: emptyList()
  }

  fun flowEntities(): Flow<List<LibraryEntity>> {
    return queries.all().asFlow().mapToList(Dispatchers.IO)
  }

  private fun convert(response: LibrariesResponse): List<LibraryEntity> {
    val entities = response.libraries.map { toEntity(it) }
    return entities
  }

  private fun cleanup(entities: List<LibraryEntity>) {
    queries.transaction {
      val ids = queries.allIds().executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }
      toDelete.forEach { queries.deleteById(it) }
    }
  }

  private fun toEntity(library: Library): LibraryEntity =
    LibraryEntity(
      id = library.id,
      name = library.name,
      folders = json.encodeToString(library.folders),
      isBookLibrary = if (library.mediaType == MediaType.BOOK) 1 else 0,
    )
}

data class PodcastFolder(val id: String, val path: String) {
  constructor(folder: Folder) : this(folder.id, folder.fullPath)
}
