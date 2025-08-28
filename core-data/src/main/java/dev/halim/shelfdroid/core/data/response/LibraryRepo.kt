package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.Library
import dev.halim.core.network.response.MediaType
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LibraryRepo @Inject constructor(private val api: ApiService, db: MyDatabase) {

  private val queries = db.libraryEntityQueries
  private val repoScope = CoroutineScope(Dispatchers.IO)

  suspend fun entities(): List<LibraryEntity> {
    val response = api.libraries().getOrNull()
    return if (response != null) {
      val entities = convert(response)
      repoScope.launch {
        cleanup(entities)
        entities.forEach { entity -> queries.insert(entity) }
      }
      entities
    } else {
      queries.all().executeAsList()
    }
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
      isBook = if (library.mediaType == MediaType.BOOK) 1 else 0,
    )
}
