package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.LibrariesResponse
import dev.halim.core.network.response.Library
import dev.halim.shelfdroid.core.database.LibraryEntity
import dev.halim.shelfdroid.core.database.LibraryEntityQueries
import javax.inject.Inject

class LibraryRepo
@Inject
constructor(private val api: ApiService, private val queries: LibraryEntityQueries) {
  fun saveAndConvert(response: LibrariesResponse): List<LibraryEntity> {
    val entities = response.libraries.map { toEntity(it) }
    entities.forEach { entity -> queries.insert(entity) }
    return entities
  }

  suspend fun entities(): List<LibraryEntity> {
    val response = api.libraries().getOrNull()
    return if (response != null) {
      saveAndConvert(response)
    } else {
      queries.all().executeAsList()
    }
  }

  fun toEntity(library: Library): LibraryEntity =
    LibraryEntity(id = library.id, name = library.name)
}
