package dev.halim.shelfdroid.core.data.response

import dev.halim.core.network.ApiService
import dev.halim.core.network.response.User
import dev.halim.core.network.response.UsersResponse
import dev.halim.shelfdroid.core.database.MyDatabase
import dev.halim.shelfdroid.core.database.UserEntity
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserRepo @Inject constructor(db: MyDatabase, private val api: ApiService) {

  private val queries = db.userEntityQueries
  private val repoScope = CoroutineScope(Dispatchers.IO)

  fun all(): List<UserEntity> = queries.all().executeAsList()

  suspend fun remote() {
    val response = api.users().getOrNull()
    if (response != null) {
      saveAndConvert(response)
    }
  }

  private fun saveAndConvert(usersResponse: UsersResponse): List<UserEntity> {
    val entities = usersResponse.users.map { toEntity(it) }
    repoScope.launch {
      cleanup(entities)
      entities.forEach { entity -> queries.insert(entity) }
    }
    return entities
  }

  private fun cleanup(entities: List<UserEntity>) {
    queries.transaction {
      val ids = queries.allIds().executeAsList()
      val newIds = entities.map { it.id }
      val toDelete = ids.filter { !newIds.contains(it) }
      toDelete.forEach { queries.deleteById(it) }
    }
  }

  private fun toEntity(user: User): UserEntity = UserEntity(id = user.id, username = user.username)
}
