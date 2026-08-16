package dev.halim.shelfdroid.core.data.library

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

class LibraryRepository
@Inject
constructor(private val api: ApiService, db: MyDatabase, private val json: Json) {

  private val queries = db.libraryEntityQueries

  fun listLibraries(): List<LibraryEntity> = queries.all().executeAsList()

  suspend fun refreshLibraries(): Result<Unit> {
    val response = api.libraries()
    val failure = response.exceptionOrNull()
    if (failure != null) return Result.failure(failure)

    return try {
      val entities = convert(response.getOrThrow())
      queries.transaction {
        cleanup(entities)
        entities.forEach { entity -> queries.insert(entity) }
      }
      Result.success(Unit)
    } catch (error: Throwable) {
      if (error is CancellationException) throw error
      Result.failure(error)
    }
  }

  fun byId(id: String): LibraryEntity? {
    return queries.byId(id).executeAsOneOrNull()
  }

  fun listLibraryFolders(id: String): List<LibraryFolder> {
    val entity = byId(id)
    if (entity == null) return emptyList()
    val folders = runCatching { json.decodeFromString<List<Folder>>(entity.folders) }
    return folders.getOrNull()?.map { LibraryFolder(it) } ?: emptyList()
  }

  fun observeLibraries(): Flow<List<LibraryEntity>> =
    queries.all().asFlow().mapToList(Dispatchers.IO)

  private fun convert(response: LibrariesResponse): List<LibraryEntity> {
    val entities = response.libraries.map { toEntity(it) }
    return entities
  }

  private fun cleanup(entities: List<LibraryEntity>) {
    val ids = queries.allIds().executeAsList()
    val newIds = entities.map { it.id }
    val toDelete = ids.filter { !newIds.contains(it) }
    toDelete.forEach { queries.deleteById(it) }
  }

  private fun toEntity(library: Library): LibraryEntity =
    LibraryEntity(
      id = library.id,
      name = library.name,
      folders = json.encodeToString(library.folders),
      isBookLibrary = if (library.mediaType == MediaType.BOOK) 1 else 0,
    )
}

data class LibraryFolder(val id: String, val path: String) {
  constructor(folder: Folder) : this(folder.id, folder.fullPath)
}
