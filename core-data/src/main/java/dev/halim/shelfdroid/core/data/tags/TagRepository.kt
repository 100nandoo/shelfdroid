package dev.halim.shelfdroid.core.data.tags

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.TagsResponse
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

class TagRepository
@Inject
constructor(private val api: ApiService, private val dataStoreManager: DataStoreManager) {

  fun listTags(): List<String> = runBlocking {
    dataStoreManager.tags.firstOrNull()?.toList()?.sortedWith(String.CASE_INSENSITIVE_ORDER)
      ?: emptyList()
  }

  suspend fun refreshTags(): Result<TagsResponse> {
    val result = api.tags()
    val response = result.getOrNull()
    if (response != null) save(response)
    return result
  }

  suspend fun save(response: TagsResponse) {
    val tags = response.tags
    dataStoreManager.updateTags(tags.toSet())
  }
}
