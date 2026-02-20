package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.TagsResponse
import dev.halim.shelfdroid.core.datastore.DataStoreManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class TagRepo
@Inject
constructor(private val api: ApiService, private val dataStoreManager: DataStoreManager) {

  private val repoScope = CoroutineScope(Dispatchers.IO)

  fun localList(): List<String> = runBlocking {
    dataStoreManager.tags.firstOrNull()?.toList()?.sorted() ?: emptyList()
  }

  suspend fun remote(): Result<TagsResponse> {
    val result = api.tags()
    val response = result.getOrNull()
    if (response != null) save(response)
    return result
  }

  private fun save(response: TagsResponse) {
    val tags = response.tags
    repoScope.launch { dataStoreManager.updateTags(tags.toSet()) }
  }
}
