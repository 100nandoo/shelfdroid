package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.Library
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LibraryRepo @Inject constructor(private val api: ApiService, db: MyDatabase) {

  private val queries = db.libraryEntityQueries

  suspend fun entities(): List<LibraryEntity> {
    val response = api.libraries().getOrNull()
    return if (response != null) {
      val entities = saveAndConvert(response)
      withContext(Dispatchers.IO) { cleanup(entities) }
      entities
    } else {
      queries.all().executeAsList()
    }
  }

  private fun saveAndConvert(response: LibrariesResponse): List<LibraryEntity> {
    val entities = response.libraries.map { toEntity(it) }
    entities.forEach { entity -> queries.insert(entity) }
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
    LibraryEntity(id = library.id, name = library.name)
}
