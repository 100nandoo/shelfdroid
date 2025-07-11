package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.Library
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.MyDatabase
import javax.inject.Inject

class LibraryRepo
@Inject
constructor(private val api: ApiService, db: MyDatabase) {

  private val queries = db.libraryEntityQueries
  suspend fun entities(): List<LibraryEntity> {
    val response = api.libraries().getOrNull()
    return if (response != null) {
      saveAndConvert(response)
    } else {
      queries.all().executeAsList()
    }
  }

  private fun saveAndConvert(response: LibrariesResponse): List<LibraryEntity> {
    val entities = response.libraries.map { toEntity(it) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  private fun toEntity(library: Library): LibraryEntity =
    LibraryEntity(id = library.id, name = library.name)
}
